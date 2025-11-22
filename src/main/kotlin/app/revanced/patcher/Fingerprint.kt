@file:Suppress("unused", "MemberVisibilityCanBePrivate")

package app.revanced.patcher

import app.revanced.patcher.Match.PatternMatch
import app.revanced.patcher.extensions.InstructionExtensions.instructionsOrNull
import app.revanced.patcher.patch.BytecodePatchContext
import app.revanced.patcher.patch.PatchException
import app.revanced.patcher.util.PatchClasses
import com.android.tools.smali.dexlib2.AccessFlags
import com.android.tools.smali.dexlib2.Opcode
import com.android.tools.smali.dexlib2.iface.ClassDef
import com.android.tools.smali.dexlib2.iface.Method
import com.android.tools.smali.dexlib2.iface.instruction.Instruction
import com.android.tools.smali.dexlib2.iface.instruction.ReferenceInstruction
import com.android.tools.smali.dexlib2.iface.reference.StringReference
import com.android.tools.smali.dexlib2.util.MethodUtil

/**
 * A fingerprint for a method. A fingerprint is a partial description of a method,
 * used to uniquely match a method by its characteristics.
 *
 * An example fingerprint for a public method that takes a single string parameter and returns void:
 * ```
 * fingerprint {
 *    accessFlags(AccessFlags.PUBLIC)
 *    returns("V")
 *    parameters("Ljava/lang/String;")
 * }
 * ```
 *
 * See the patcher documentation for more detailed explanations and example fingerprinting.
 *
 * @param accessFlags The exact access flags using values of [AccessFlags].
 * @param returnType The return type. Compared using [String.startsWith].
 * @param parameters The parameters. Partial matches allowed and follow the same rules as [returnType].
 * @param filters A list of filters to match, declared in the same order the instructions appear in the method.
 * @param strings A list of strings that appear anywhere in the method in any order. Compared using [String.contains].
 * @param custom A custom condition for this fingerprint.
 */
class Fingerprint internal constructor(
    internal val accessFlags: Int?,
    internal val returnType: String?,
    internal val parameters: List<String>?,
    internal val filters: List<InstructionFilter>?,
    // TODO: Possibly deprecate legacy string declarations in the future.
    internal val strings: List<String>?,
    internal val custom: ((method: Method, classDef: ClassDef) -> Boolean)?,
) {
    @Suppress("ktlint:standard:backing-property-naming")
    // Backing field needed for lazy initialization.
    private var _matchOrNull: Match? = null

    /**
     * Clears the current match, forcing this fingerprint to resolve again.
     * This method should only be used if this fingerprint is re-used after it's modified,
     * and the prior match indexes are no longer correct.
     */
    fun clearMatch() {
        _matchOrNull = null
    }

    /**
     * The match for this [Fingerprint], or `null` if no matches exist.
     */
    context(BytecodePatchContext)
    fun matchOrNull(): Match? {
        if (_matchOrNull != null) return _matchOrNull

        // Use string declarations to first check only the classes
        // that contain one or more fingerprint strings.
        val fingerprintStrings = mutableListOf<String>()
        if (strings != null) {
            // Old deprecated string declaration.
            fingerprintStrings.addAll(strings)
        }

        if (filters != null) {
            fun findStringFilterLiterals(list: List<InstructionFilter>) =
                list.filterIsInstance<StringFilter>().map { it.string() }

            fingerprintStrings.addAll(findStringFilterLiterals(filters))

            // Use strings declared inside anyInstruction.
            filters.filterIsInstance<AnyInstruction>().forEach { anyFilter ->
                fingerprintStrings.addAll(findStringFilterLiterals(anyFilter.filters))
            }
        }

        fun machAllClassMethods(value: PatchClasses.ClassDefWrapper): Match? {
            val classDef = value.classDef
            value.classDef.methods.forEach { method ->
                val match = matchOrNull(classDef, method)
                if (match != null) {
                    _matchOrNull = match
                    return match
                }
            }
            return null
        }

        if (fingerprintStrings.isNotEmpty()) {
            fingerprintStrings.forEach { string ->
                classes.getClassFromOpcodeStringLiteral(string)?.forEach { stringClass ->
                    val value = machAllClassMethods(stringClass)
                    if (value != null) {
                        return value
                    }
                }
            }

            // Fingerprint has partial string matches. Check all classes with strings.
            classes.getAllClassesWithStrings().forEach { value ->
                val value = machAllClassMethods(value)
                if (value != null) {
                    return value
                }
            }
        }

        // Check all classes.
        classes.classMap.values.forEach { value ->
            val value = machAllClassMethods(value)
            if (value != null) {
                return value
            }
        }

        return null
    }

    /**
     * Match using a [ClassDef].
     *
     * @param classDef The class to match against.
     * @return The [Match] if a match was found or if the
     * fingerprint is already matched to a method, null otherwise.
     */
    context(BytecodePatchContext)
    fun matchOrNull(
        classDef: ClassDef,
    ): Match? {
        if (_matchOrNull != null) return _matchOrNull

        for (method in classDef.methods) {
            val match = matchOrNull(classDef, method)
            if (match != null) {
                _matchOrNull = match
                return match
            }
        }

        return null
    }

    /**
     * Match using a [Method].
     * The class is retrieved from the method.
     *
     * @param method The method to match against.
     * @return The [Match] if a match was found or if the fingerprint is previously matched to a method,
     * otherwise `null`.
     */
    context(BytecodePatchContext)
    fun matchOrNull(
        method: Method,
    ): Match? {
        if (_matchOrNull != null) return _matchOrNull

        return matchOrNull(classBy(method.definingClass), method)
    }

    /**
     * Match using a [Method].
     *
     * @param method The method to match against.
     * @param classDef The class the method is a member of.
     * @return The [Match] if a match was found or if the fingerprint is previously matched to a method,
     *         otherwise `null`.
     */
    context(BytecodePatchContext)
    fun matchOrNull(
        classDef: ClassDef,
        method: Method,
    ): Match? {
        if (_matchOrNull != null) return _matchOrNull

        if (returnType != null && !method.returnType.startsWith(returnType)) {
            return null
        }

        if (accessFlags != null && accessFlags != method.accessFlags) {
            return null
        }

        // TODO: parseParameters()
        if (parameters != null && !parametersStartsWith(method.parameterTypes, parameters)) {
            return null
        }

        if (custom != null && !custom.invoke(method, classDef)) {
            return null
        }

        // Legacy string declarations.
        val stringMatches: List<Match.StringMatch>? = if (strings == null) {
            null
        } else {
            buildList {
                val instructions = method.instructionsOrNull ?: return null

                var stringsList : MutableList<String>? = null

                instructions.forEachIndexed { instructionIndex, instruction ->
                    if (
                        instruction.opcode != Opcode.CONST_STRING &&
                        instruction.opcode != Opcode.CONST_STRING_JUMBO
                    ) {
                        return@forEachIndexed
                    }

                    val string = ((instruction as ReferenceInstruction).reference as StringReference).string
                    if (stringsList == null) {
                        stringsList = strings.toMutableList()
                    }
                    val index = stringsList.indexOfFirst(string::contains)
                    if (index < 0) return@forEachIndexed

                    add(Match.StringMatch(string, instructionIndex))
                    stringsList.removeAt(index)
                }

                if (stringsList == null || stringsList.isNotEmpty()) return null
            }
        }

        val instructionMatches = if (filters == null) {
            null
        } else {
            val instructions = method.instructionsOrNull?.toList() ?: return null

            fun matchFilters(): List<Match.InstructionMatch>? {
                val lastMethodIndex = instructions.lastIndex
                var instructionMatches : MutableList<Match.InstructionMatch>? = null

                var firstInstructionIndex = 0
                var lastMatchIndex = -1

                firstFilterLoop@ while (true) {
                    // Matched index of the first filter.
                    var firstFilterIndex = -1
                    var subIndex = firstInstructionIndex

                    for (filterIndex in filters.indices) {
                        val filter = filters[filterIndex]
                        val location = filter.location
                        var instructionsMatched = false

                        while (subIndex <= lastMethodIndex &&
                            location.indexIsValidForMatching(
                                lastMatchIndex, subIndex
                            )
                        ) {
                            val instruction = instructions[subIndex]
                            if (filter.matches(method, instruction)) {
                                lastMatchIndex = subIndex

                                if (filterIndex == 0) {
                                    firstFilterIndex = subIndex
                                }
                                if (instructionMatches == null) {
                                    instructionMatches = ArrayList(filters.size)
                                }
                                instructionMatches += Match.InstructionMatch(filter, subIndex, instruction)
                                instructionsMatched = true
                                subIndex++
                                break
                            }
                            subIndex++
                        }

                        if (!instructionsMatched) {
                            if (filterIndex == 0) {
                                return null // First filter has no more matches to start from.
                            }

                            // Try again with the first filter, starting from
                            // the next possible first filter index.
                            firstInstructionIndex = firstFilterIndex + 1
                            instructionMatches?.clear()
                            continue@firstFilterLoop
                        }
                    }

                    // All instruction filters matches.
                    return instructionMatches
                }
            }

            matchFilters() ?: return null
        }

        _matchOrNull = Match(
            classDef,
            method,
            instructionMatches,
            stringMatches,
        )

        return _matchOrNull
    }

    fun patchException() = PatchException("Failed to match the fingerprint: $this")

    /**
     * The match for this [Fingerprint].
     *
     * @return The [Match] of this fingerprint.
     * @throws PatchException If the [Fingerprint] failed to match.
     */
    context(BytecodePatchContext)
    fun match() = matchOrNull() ?: throw patchException()

    /**
     * Match using a [ClassDef].
     *
     * @param classDef The class to match against.
     * @return The [Match] of this fingerprint.
     * @throws PatchException If the fingerprint failed to match.
     */
    context(BytecodePatchContext)
    fun match(
        classDef: ClassDef,
    ) = matchOrNull(classDef) ?: throw patchException()

    /**
     * Match using a [Method].
     * The class is retrieved from the method.
     *
     * @param method The method to match against.
     * @return The [Match] of this fingerprint.
     * @throws PatchException If the fingerprint failed to match.
     */
    context(BytecodePatchContext)
    fun match(
        method: Method,
    ) = matchOrNull(method) ?: throw patchException()

    /**
     * Match using a [Method].
     *
     * @param method The method to match against.
     * @param classDef The class the method is a member of.
     * @return The [Match] of this fingerprint.
     * @throws PatchException If the fingerprint failed to match.
     */
    context(BytecodePatchContext)
    fun match(
        method: Method,
        classDef: ClassDef,
    ) = matchOrNull(classDef, method) ?: throw patchException()

    /**
     * The class the matching method is a member of, or null if this fingerprint did not match.
     */
    context(BytecodePatchContext)
    val originalClassDefOrNull
        get() = matchOrNull()?.originalClassDef

    /**
     * The matching method, or null of this fingerprint did not match.
     */
    context(BytecodePatchContext)
    val originalMethodOrNull
        get() = matchOrNull()?.originalMethod

    /**
     * The mutable version of [originalClassDefOrNull].
     *
     * Accessing this property allocates a new mutable instance.
     * Use [originalClassDefOrNull] if mutable access is not required.
     */
    context(BytecodePatchContext)
    val classDefOrNull
        get() = matchOrNull()?.classDef

    /**
     * The mutable version of [originalMethodOrNull].
     *
     * Accessing this property allocates a new mutable instance.
     * Use [originalMethodOrNull] if mutable access is not required.
     */
    context(BytecodePatchContext)
    val methodOrNull
        get() = matchOrNull()?.method

    /**
     * The match for the instruction filters, or null if this fingerprint did not match.
     */
    context(BytecodePatchContext)
    val instructionMatchesOrNull
        get() = matchOrNull()?.instructionMatchesOrNull

    /**
     * The class the matching method is a member of.
     *
     * @throws PatchException If the fingerprint has not been matched.
     */
    context(BytecodePatchContext)
    val originalClassDef
        get() = match().originalClassDef

    /**
     * The matching method.
     *
     * @throws PatchException If the fingerprint has not been matched.
     */
    context(BytecodePatchContext)
    val originalMethod
        get() = match().originalMethod

    /**
     * The mutable version of [originalClassDef].
     *
     * Accessing this property allocates a new mutable instance.
     * Use [originalClassDef] if mutable access is not required.
     *
     * @throws PatchException If the fingerprint has not been matched.
     */
    context(BytecodePatchContext)
    val classDef
        get() = match().classDef

    /**
     * The mutable version of [originalMethod].
     *
     * Accessing this property allocates a new mutable instance.
     * Use [originalMethod] if mutable access is not required.
     *
     * @throws PatchException If the fingerprint has not been matched.
     */
    context(BytecodePatchContext)
    val method
        get() = match().method

    /**
     * Instruction filter matches.
     *
     * @throws PatchException If the fingerprint has not been matched.
     */
    context(BytecodePatchContext)
    val instructionMatches
        get() = match().instructionMatches

    /**
     * The matches for the strings declared using `strings()`.
     * This does not give matches for strings declared using [string] instruction filters.
     *
     * @throws PatchException If the fingerprint has not been matched.
     */
    // TODO: Possibly deprecate this in the future.
    context(BytecodePatchContext)
    val stringMatches
        get() = match().stringMatches

    //
    // Old legacy non-unified matching objects.
    //

    /**
     * The matches for strings declared in [Fingerprint.strings].
     *
     * **Note**: Strings declared as instruction filters are not included in these legacy match results.
     *
     * This property may be deprecated in the future.
     * Consider changing to [InstructionFilter] and [string] declarations.
     */
    // TODO: Possibly deprecate this in the future.
    context(BytecodePatchContext)
    val stringMatchesOrNull
        get() = matchOrNull()?.stringMatchesOrNull

    /**
     * Instead use `instructionMatches`.
     *
     * The opcode pattern start index is:
     * `.instructionMatches.first().index`
     *
     * and the end index is:
     * `instructionMatches.last().index`
     *
     * @throws PatchException If the fingerprint has not been matched.
     */
    context(BytecodePatchContext)
    @Deprecated("Instead use instructionMatches", ReplaceWith("instructionMatches"))
    val patternMatch
        get() = match().patternMatch

    /**
     * Instead use `instructionMatches`.
     *
     * The opcode pattern start index is:
     * `.instructionMatches.first().index`
     *
     * and the end index is:
     * `instructionMatches.last().index`
     */
    context(BytecodePatchContext)
    @Deprecated("instead use instructionMatchesOrNull", ReplaceWith("instructionMatchesOrNull"))
    val patternMatchOrNull : PatternMatch?
        get() {
            val match = this.matchOrNull()
            if (match == null || match.instructionMatchesOrNull == null) {
                return null
            }
            return match.patternMatch
        }
}

/**
 * A match of a [Fingerprint].
 *
 * @param originalClassDef The class the matching method is a member of.
 * @param originalMethod The matching method.
 * @param _instructionMatches The match for the instruction filters.
 * @param _stringMatches The matches for the strings declared using `strings()`.
 */
context(BytecodePatchContext)
class Match internal constructor(
    val originalClassDef: ClassDef,
    val originalMethod: Method,
    private val _instructionMatches: List<InstructionMatch>?,
    private val _stringMatches: List<StringMatch>?,
) {
    /**
     * The mutable version of [originalClassDef].
     *
     * Accessing this property allocates a new mutable instance.
     * Use [originalClassDef] if mutable access is not required.
     */
    val classDef by lazy { mutableClassBy(originalClassDef) }

    /**
     * The mutable version of [originalMethod].
     *
     * Accessing this property allocates a new mutable instance.
     * Use [originalMethod] if mutable access is not required.
     */
    val method by lazy { classDef.methods.first { MethodUtil.methodSignaturesMatch(it, originalMethod) } }

    /**
     * Matches corresponding to the [InstructionFilter] declared in the [Fingerprint].
     */
    val instructionMatches
        get() = _instructionMatches ?: throw PatchException("Fingerprint declared no instruction filters")
    val instructionMatchesOrNull = _instructionMatches

    /**
     * A match for an [InstructionFilter].
     * @param filter The filter that matched
     * @param index The instruction index it matched with.
     * @param instruction The instruction that matched.
     */
    class InstructionMatch internal constructor(
        val filter : InstructionFilter,
        val index: Int,
        val instruction: Instruction
    ) {
        /**
         * Helper method to simplify casting the instruction to it's known and expected type.
         */
        @Suppress("UNCHECKED_CAST")
        fun <T> getInstruction(): T = instruction as T

        override fun toString(): String {
            return "InstructionMatch{filter='${filter.javaClass.simpleName}, opcode='${instruction.opcode}, 'index=$index}"
        }
    }

    //
    // Old legacy non-unified matching objects.
    //

    /**
     * The matches for strings declared in [Fingerprint.strings].
     *
     * **Note**: Strings declared as instruction filters are not included in these legacy match results.
     *
     * This property may be deprecated in the future.
     * Consider changing to [InstructionFilter] and [string] declarations.
     */
    // TODO: Possibly deprecate this in the future.
    val stringMatches
        get() = _stringMatches ?: throw PatchException("Fingerprint declared no strings")
    val stringMatchesOrNull = _stringMatches

    /**
     * A match for a string declared in [Fingerprint.stringMatches].
     *
     * **Note**: Strings declared as instruction filters are not included in this legacy match object.
     *
     * This legacy match type may be deprecated in the future.
     * Consider changing to [InstructionFilter] and [StringFilter] declarations.
     *
     * @param string The string that matched.
     * @param index The index of the instruction in the method.
     */
    // TODO: Possibly deprecate this in the future.
    class StringMatch internal constructor(val string: String, val index: Int)

    /**
     * Instead use `instructionMatches`.
     *
     * The opcode pattern start index is:
     * `.instructionMatches.first().index`
     *
     * and the end index is:
     * `instructionMatches.last().index`
     */
    @Deprecated("Instead use instructionMatches", ReplaceWith("instructionMatches"))
    val patternMatch by lazy {
        if (_instructionMatches == null) throw PatchException("Did not match $this")
        @SuppressWarnings("deprecation")
        PatternMatch(_instructionMatches.first().index, _instructionMatches.last().index)
    }

    /**
     * A legacy match result.
     *
     * Instead use `instructionMatches`.
     * The opcode pattern start index is:
     * `.instructionMatches.first().index`
     *
     * and the end index is:
     * `instructionMatches.last().index`
     *
     * @param startIndex The index of the first opcode of the pattern in the method.
     * @param endIndex The index of the last opcode of the pattern in the method.
     */
    @Deprecated("Instead use InstructionMatch")
    class PatternMatch internal constructor(
        val startIndex: Int,
        val endIndex: Int,
    )
}

/**
 * A builder for [Fingerprint].
 *
 * @property accessFlags The exact access flags using values of [AccessFlags].
 * @property returnType The return type compared using [String.startsWith].
 * @property parameters The parameters of the method. Partial matches allowed and follow the same rules as [returnType].
 * @property instructionFilters Filters to match the method instructions.
 * @property strings A list of the strings compared each using [String.contains].
 * @property customBlock A custom condition for this fingerprint.
 *
 * @constructor Create a new [FingerprintBuilder].
 */
class FingerprintBuilder() {
    private var accessFlags: Int? = null
    private var returnType: String? = null
    private var parameters: List<String>? = null
    private var instructionFilters: List<InstructionFilter>? = null
    private var strings: List<String>? = null
    private var customBlock: ((method: Method, classDef: ClassDef) -> Boolean)? = null

    /**
     * Set the access flags.
     *
     * @param accessFlags The exact access flags using values of [AccessFlags].
     */
    fun accessFlags(accessFlags: Int) {
        this.accessFlags = accessFlags
    }

    /**
     * Set the access flags.
     *
     * @param accessFlags The exact access flags using values of [AccessFlags].
     */
    fun accessFlags(vararg accessFlags: AccessFlags) {
        this.accessFlags = accessFlags.fold(0) { acc, it -> acc or it.value }
    }

    /**
     * Set the return type.
     *
     * If [accessFlags] includes [AccessFlags.CONSTRUCTOR], then there is no need to
     * set a return type set since constructors are always void return type.
     *
     * @param returnType The return type compared using [String.startsWith].
     */
    fun returns(returnType: String) {
        this.returnType = returnType
    }

    /**
     * Set the parameters.
     *
     * @param parameters The parameters of the method.
     *                   Partial matches allowed and follow the same rules as [returnType].
     */
    fun parameters(vararg parameters: String) {
        this.parameters = parameters.toList()
    }

    private fun verifyNoFiltersSet() {
        if (this.instructionFilters != null) {
            throw PatchException("Instruction filters already set")
        }
    }

    /**
     * A pattern of opcodes, where each opcode must appear immediately after the previous.
     *
     * To use opcodes with other [InstructionFilter] objects,
     * instead use [instructions] with individual opcodes declared using [opcode].
     *
     * This method is identical to declaring individual opcode filters
     * with [InstructionFilter.location] set to [InstructionLocation.MatchAfterImmediately]
     * for all but the first opcode.
     *
     * Unless absolutely necessary, it is recommended to instead use [instructions]
     * with more fine grained filters.
     *
     * ```
     * opcodes(
     *    Opcode.INVOKE_VIRTUAL, // First opcode matches anywhere in the method.
     *    Opcode.MOVE_RESULT_OBJECT, // Must match exactly after INVOKE_VIRTUAL.
     *    Opcode.IPUT_OBJECT // Must match exactly after MOVE_RESULT_OBJECT.
     * )
     * ```
     * is identical to:
     * ```
     * instructions(
     *    opcode(Opcode.INVOKE_VIRTUAL), // First opcode matches anywhere in the method.
     *    opcode(Opcode.MOVE_RESULT_OBJECT, MatchAfterImmediately()), // Must match exactly after INVOKE_VIRTUAL.
     *    opcode(Opcode.IPUT_OBJECT, MatchAfterImmediately()) // Must match exactly after MOVE_RESULT_OBJECT.
     * )
     * ```
     *
     * @param opcodes An opcode pattern of instructions.
     *                Wildcard or unknown opcodes can be specified by `null`.
     */
    fun opcodes(vararg opcodes: Opcode?) {
        verifyNoFiltersSet()
        if (opcodes.isEmpty()) throw IllegalArgumentException("One or more opcodes is required")

        this.instructionFilters = OpcodesFilter.listOfOpcodes(opcodes.toList())
    }

    /**
     * A pattern of opcodes from SMALI formatted text,
     * where each opcode must appear immediately after the previous opcode.
     *
     * Unless absolutely necessary, it is recommended to instead use [instructions].
     *
     * @param instructions A list of instructions or opcode names in SMALI format.
     * - Wildcard or unknown opcodes can be specified by `null`.
     * - Empty lines are ignored.
     * - Each instruction must be on a new line.
     * - The opcode name is enough, no need to specify the operands.
     *
     * @throws Exception If an unknown opcode is used.
     */
    fun opcodes(instructions: String) {
        verifyNoFiltersSet()
        if (instructions.isBlank()) throw IllegalArgumentException("No instructions declared (empty string)")

        this.instructionFilters = OpcodesFilter.listOfOpcodes(
            instructions.trimIndent().split("\n").filter {
                it.isNotBlank()
            }.map {
                // Remove any operands.
                val name = it.split(" ", limit = 1).first().trim()
                if (name == "null") return@map null

                opcodesByName[name] ?: throw IllegalArgumentException("Unknown opcode: $name")
            }
        )
    }

    /**
     * A list of instruction filters to match.
     */
    fun instructions(vararg instructionFilters: InstructionFilter) {
        verifyNoFiltersSet()
        if (instructionFilters.isEmpty()) throw IllegalArgumentException("One or more instructions is required")

        this.instructionFilters = instructionFilters.toList()
    }

    /**
     * Set the strings.
     *
     * @param strings A list of strings compared each using [String.contains].
     */
    @Deprecated("Instead use `instruction()` filters and `string()` instruction declarations")
    fun strings(vararg strings: String) {
        this.strings = strings.toList()
    }

    /**
     * Set a custom condition for this fingerprint.
     *
     * @param customBlock A custom condition for this fingerprint.
     */
    fun custom(customBlock: (method: Method, classDef: ClassDef) -> Boolean) {
        this.customBlock = customBlock
    }

    fun build(): Fingerprint {
        // If access flags include constructor then
        // skip the return type check since it's always void.
        if (returnType?.equals("V") == true && accessFlags != null
            && AccessFlags.CONSTRUCTOR.isSet(accessFlags!!)
        ) {
            returnType = null
        }

        return Fingerprint(
            accessFlags,
            returnType,
            parameters,
            instructionFilters,
            strings,
            customBlock,
        )
    }


    private companion object {
        val opcodesByName = Opcode.entries.associateBy { it.name }
    }
}

fun fingerprint(
    block: FingerprintBuilder.() -> Unit,
) = FingerprintBuilder().apply(block).build()

/**
 * Matches two lists of parameters, where the first parameter list
 * starts with the values of the second list.
 */
internal fun parametersStartsWith(
    targetMethodParameters: Iterable<CharSequence>,
    fingerprintParameters: Iterable<CharSequence>,
): Boolean {
    if (fingerprintParameters.count() != targetMethodParameters.count()) return false
    val fingerprintIterator = fingerprintParameters.iterator()

    targetMethodParameters.forEach {
        if (!it.startsWith(fingerprintIterator.next())) return false
    }

    return true
}
