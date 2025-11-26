@file:Suppress("unused", "MemberVisibilityCanBePrivate")

package app.revanced.patcher

import app.revanced.patcher.InstructionLocation.*
import app.revanced.patcher.Match.PatternMatch
import app.revanced.patcher.Matcher.MatchContext
import app.revanced.patcher.extensions.getInstruction
import app.revanced.patcher.extensions.instructionsOrNull
import app.revanced.patcher.extensions.opcode
import app.revanced.patcher.extensions.string
import app.revanced.patcher.extensions.stringReference
import app.revanced.patcher.patch.BytecodePatchContext
import app.revanced.patcher.patch.PatchException
import com.android.tools.smali.dexlib2.AccessFlags
import com.android.tools.smali.dexlib2.Opcode
import com.android.tools.smali.dexlib2.iface.ClassDef
import com.android.tools.smali.dexlib2.iface.Method
import com.android.tools.smali.dexlib2.iface.instruction.Instruction
import com.android.tools.smali.dexlib2.util.MethodUtil

/**
 * A fingerprint for a method. A fingerprint is a partial description of a method.
 * It is used to uniquely match a method by its characteristics.
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
 * @param accessFlags The exact access flags using values of [AccessFlags].
 * @param returnType The return type. Compared using [String.startsWith].
 * @param parameters The parameters. Partial matches allowed and follow the same rules as [returnType].
 * @param filters A list of filters to match, declared in the same order the instructions appear in the method.
 * @param strings A list of the strings that appear anywhere in the method. Compared using [String.contains].
 * @param custom A custom condition for this fingerprint.
 */
class Fingerprint internal constructor(
    internal val accessFlags: Int?,
    internal val returnType: String?,
    internal val parameters: List<String>?,
    internal val filters: List<InstructionFilter>?,
    @Deprecated("Instead use instruction filters")
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
    context(context: BytecodePatchContext)
    fun matchOrNull(): Match? {
        if (_matchOrNull != null) return _matchOrNull

        var stringMatches: List<Match.StringMatch>? = null

        val matchIndices = indexedMatcher<Instruction>()

        // This line is needed, because the method must be passed by reference to "matchIndices".
        // Referencing the method directly would "hardcode" it in the cached pattern by value.
        // By using this variable, the reference can be updated for each method.
        lateinit var currentMethod: Method

        context(_: MatchContext)
        fun Method.match(): Boolean {
            if (this@Fingerprint.accessFlags != null && this@Fingerprint.accessFlags != accessFlags)
                return false

            if (this@Fingerprint.returnType != null && !returnType.startsWith(this@Fingerprint.returnType))
                return false

            if (this@Fingerprint.parameters != null && !parametersStartsWith(
                    parameterTypes,
                    this@Fingerprint.parameters
                )
            )
                return false

            if (custom != null && !custom(this, context.lookupMaps.classDefsByType[definingClass]!!))
                return false

            stringMatches = if (strings != null) {
                val instructions = instructionsOrNull ?: return false
                var stringsList: MutableList<String>? = null

                buildList {
                    instructions.forEachIndexed { instructionIndex, instruction ->
                        if (stringsList == null) stringsList = strings.toMutableList()

                        val string = instruction.stringReference?.string ?: return@forEachIndexed
                        val index = stringsList.indexOfFirst(string::contains)
                        if (index < 0) return@forEachIndexed

                        add(Match.StringMatch(string, instructionIndex))
                        stringsList.removeAt(index)
                    }

                    if (stringsList == null || stringsList.isNotEmpty()) return false
                }

            } else null

            currentMethod = this
            return filters == null || matchIndices(instructionsOrNull ?: return false, "match") {
                filters.forEach { filter ->
                    val filterMatches: Instruction.() -> Boolean = { filter.matches(currentMethod, this) }

                    when (val location = filter.location) {
                        is MatchAfterImmediately -> after { filterMatches() }
                        is MatchAfterWithin -> after(1..location.matchDistance) { filterMatches() }
                        is MatchAfterAnywhere -> add { filterMatches() }
                        is MatchAfterAtLeast -> after(location.minimumDistanceFromLastInstruction..Int.MAX_VALUE) { filterMatches() }
                        is MatchAfterRange -> after(location.minimumDistanceFromLastInstruction..location.maximumDistanceFromLastInstruction) { filterMatches() }
                        is MatchFirst -> head { filterMatches() }
                    }
                }
            }
        }

        val allStrings = buildList {
            if (filters != null) addAll(
                (filters.filterIsInstance<StringFilter>() + filters.filterIsInstance<AnyInstruction>().flatMap {
                    it.filters.filterIsInstance<StringFilter>()
                })
            )
        }.map { it.stringValue } + (strings ?: emptyList())

        val method = if (allStrings.isNotEmpty()) {
            context.firstMethodOrNull(strings = allStrings.toTypedArray()) { match() }
                ?: context(MatchContext()) { context.lookupMaps.methodsWithString.firstOrNull { it.match() } }
        } else {
            context.firstMethodOrNull { match() }
        } ?: return null

        val instructionMatches = filters?.withIndex()?.map { (i, filter) ->
            val matchIndex = matchIndices.indices[i]

            Match.InstructionMatch(filter, matchIndex, method.getInstruction(matchIndex))
        }

        _matchOrNull = Match(
            context,
            context.lookupMaps.classDefsByType[method.definingClass]!!,
            method,
            instructionMatches,
            stringMatches,
        )

        return _matchOrNull
    }

    /**
     * Match using a [ClassDef].
     *
     * @param classDef The class to match against.
     * @return The [Match] if a match was found or if the
     * fingerprint is already matched to a method, null otherwise.
     */
    context(_: BytecodePatchContext)
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
    context(context: BytecodePatchContext)
    fun matchOrNull(
        method: Method,
    ): Match? {
        if (_matchOrNull != null) return _matchOrNull

        return matchOrNull(context.lookupMaps.classDefsByType[method.definingClass]!!, method)
    }

    /**
     * Match using a [Method].
     *
     * @param method The method to match against.
     * @param classDef The class the method is a member of.
     * @return The [Match] if a match was found or if the fingerprint is previously matched to a method,
     * otherwise `null`.
     */
    context(context: BytecodePatchContext)
    fun matchOrNull(
        classDef: ClassDef,
        method: Method,
    ): Match? {
        if (_matchOrNull != null) return _matchOrNull

        var stringMatches: List<Match.StringMatch>? = null

        val matchIndices = indexedMatcher<Instruction>()

        context(_: MatchContext)
        fun Method.match(): Boolean {
            if (this@Fingerprint.accessFlags != null && this@Fingerprint.accessFlags != accessFlags)
                return false

            if (this@Fingerprint.returnType != null && !returnType.startsWith(this@Fingerprint.returnType))
                return false

            if (this@Fingerprint.parameters != null && !parametersStartsWith(
                    parameterTypes,
                    this@Fingerprint.parameters
                )
            )
                return false

            if (custom != null && !custom(this, classDef))
                return false

            stringMatches = if (strings != null) {
                val instructions = instructionsOrNull ?: return false
                var stringsList: MutableList<String>? = null

                buildList {
                    instructions.forEachIndexed { instructionIndex, instruction ->
                        if (stringsList == null) stringsList = strings.toMutableList()

                        val string = instruction.stringReference?.string ?: return@forEachIndexed
                        val index = stringsList.indexOfFirst(string::contains)
                        if (index < 0) return@forEachIndexed

                        add(Match.StringMatch(string, instructionIndex))
                        stringsList.removeAt(index)
                    }

                    if (stringsList == null || stringsList.isNotEmpty()) return false
                }

            } else null

            return filters == null || matchIndices.apply {
                filters.forEach { filter ->
                    val filterMatches: Instruction.() -> Boolean = { filter.matches(method, this) }

                    when (val location = filter.location) {
                        is MatchAfterImmediately -> after { filterMatches() }
                        is MatchAfterWithin -> after(1..location.matchDistance) { filterMatches() }
                        is MatchAfterAnywhere -> add { filterMatches() }
                        is MatchAfterAtLeast -> after(location.minimumDistanceFromLastInstruction..Int.MAX_VALUE) { filterMatches() }
                        is MatchAfterRange -> after(location.minimumDistanceFromLastInstruction..location.maximumDistanceFromLastInstruction) { filterMatches() }
                        is MatchFirst -> head { filterMatches() }
                    }
                }
            }(instructionsOrNull ?: return false)
        }

        if (!context(MatchContext()) { method.match() }) return null
        val instructionMatches = filters?.withIndex()?.map { (i, filter) ->
            val matchIndex = matchIndices.indices[i]
            Match.InstructionMatch(filter, matchIndex, method.getInstruction(matchIndex))
        }

        _matchOrNull = Match(
            context,
            context.lookupMaps.classDefsByType[method.definingClass]!!,
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
    context(_: BytecodePatchContext)
    fun match() = matchOrNull() ?: throw patchException()

    /**
     * Match using a [ClassDef].
     *
     * @param classDef The class to match against.
     * @return The [Match] of this fingerprint.
     * @throws PatchException If the fingerprint failed to match.
     */
    context(_: BytecodePatchContext)
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
    context(_: BytecodePatchContext)
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
    context(_: BytecodePatchContext)
    fun match(
        method: Method,
        classDef: ClassDef,
    ) = matchOrNull(classDef, method) ?: throw patchException()

    /**
     * The class the matching method is a member of, or null if this fingerprint did not match.
     */
    context(_: BytecodePatchContext)
    val originalClassDefOrNull
        get() = matchOrNull()?.originalClassDef

    /**
     * The matching method, or null of this fingerprint did not match.
     */
    context(_: BytecodePatchContext)
    val originalMethodOrNull
        get() = matchOrNull()?.originalMethod

    /**
     * The mutable version of [originalClassDefOrNull].
     *
     * Accessing this property allocates a new mutable instance.
     * Use [originalClassDefOrNull] if mutable access is not required.
     */
    context(_: BytecodePatchContext)
    val classDefOrNull
        get() = matchOrNull()?.classDef

    /**
     * The mutable version of [originalMethodOrNull].
     *
     * Accessing this property allocates a new mutable instance.
     * Use [originalMethodOrNull] if mutable access is not required.
     */
    context(_: BytecodePatchContext)
    val methodOrNull
        get() = matchOrNull()?.method

    /**
     * The match for the opcode pattern, or null if this fingerprint did not match.
     */
    context(_: BytecodePatchContext)
    @Deprecated("instead use instructionMatchesOrNull")
    val patternMatchOrNull: PatternMatch?
        get() {
            val match = this.matchOrNull()
            if (match == null || match.instructionMatchesOrNull == null) {
                return null
            }
            return match.patternMatch
        }

    /**
     * The match for the instruction filters, or null if this fingerprint did not match.
     */
    context(_: BytecodePatchContext)
    val instructionMatchesOrNull
        get() = matchOrNull()?.instructionMatchesOrNull

    /**
     * The matches for the strings, or null if this fingerprint did not match.
     *
     * This does not give matches for strings declared using [string] instruction filters.
     */
    context(_: BytecodePatchContext)
    @Deprecated("Instead use string instructions and `instructionMatchesOrNull()`")
    val stringMatchesOrNull
        get() = matchOrNull()?.stringMatchesOrNull

    /**
     * The class the matching method is a member of.
     *
     * @throws PatchException If the fingerprint has not been matched.
     */
    context(_: BytecodePatchContext)
    val originalClassDef
        get() = match().originalClassDef

    /**
     * The matching method.
     *
     * @throws PatchException If the fingerprint has not been matched.
     */
    context(_: BytecodePatchContext)
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
    context(_: BytecodePatchContext)
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
    context(_: BytecodePatchContext)
    val method
        get() = match().method

    /**
     * The match for the opcode pattern.
     *
     * @throws PatchException If the fingerprint has not been matched.
     */
    context(_: BytecodePatchContext)
    @Deprecated("Instead use instructionMatch")
    val patternMatch
        get() = match().patternMatch

    /**
     * Instruction filter matches.
     *
     * @throws PatchException If the fingerprint has not been matched.
     */
    context(_: BytecodePatchContext)
    val instructionMatches
        get() = match().instructionMatches

    /**
     * The matches for the strings declared using `strings()`.
     * This does not give matches for strings declared using [string] instruction filters.
     *
     * @throws PatchException If the fingerprint has not been matched.
     */
    context(_: BytecodePatchContext)
    @Deprecated("Instead use string instructions and `instructionMatches()`")
    val stringMatches
        get() = match().stringMatches
}

/**
 * A match of a [Fingerprint].
 *
 * @param originalClassDef The class the matching method is a member of.
 * @param originalMethod The matching method.
 * @param _instructionMatches The match for the instruction filters.
 * @param _stringMatches The matches for the strings declared using `strings()`.
 */
class Match internal constructor(
    val context: BytecodePatchContext,
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
    val classDef by lazy { with(context) { originalClassDef.mutable() } }

    /**
     * The mutable version of [originalMethod].
     *
     * Accessing this property allocates a new mutable instance.
     * Use [originalMethod] if mutable access is not required.
     */
    val method by lazy { classDef.methods.first { MethodUtil.methodSignaturesMatch(it, originalMethod) } }

    @Deprecated("Instead use instructionMatches", ReplaceWith("instructionMatches"))
    val patternMatch by lazy {
        if (_instructionMatches == null) throw PatchException("Did not match $this")
        @SuppressWarnings("deprecation")
        PatternMatch(_instructionMatches.first().index, _instructionMatches.last().index)
    }

    val instructionMatches
        get() = _instructionMatches ?: throw PatchException("Fingerprint declared no instruction filters")
    val instructionMatchesOrNull = _instructionMatches

    @Deprecated("Instead use string instructions and `instructionMatches()`")
    val stringMatches
        get() = _stringMatches ?: throw PatchException("Fingerprint declared no strings")

    @Deprecated("Instead use string instructions and `instructionMatchesOrNull()`")
    val stringMatchesOrNull = _stringMatches

    /**
     * A match for an opcode pattern.
     * @param startIndex The index of the first opcode of the pattern in the method.
     * @param endIndex The index of the last opcode of the pattern in the method.
     */
    @Deprecated("Instead use InstructionMatch")
    class PatternMatch internal constructor(
        val startIndex: Int,
        val endIndex: Int,
    )

    /**
     * A match for a string.
     *
     * @param string The string that matched.
     * @param index The index of the instruction in the method.
     */
    @Deprecated("Instead use string instructions and `InstructionMatch`")
    class StringMatch internal constructor(val string: String, val index: Int)

    /**
     * A match for a [InstructionFilter].
     * @param filter The filter that matched
     * @param index The instruction index it matched with.
     * @param instruction The instruction that matched.
     */
    class InstructionMatch internal constructor(
        val filter: InstructionFilter,
        val index: Int,
        val instruction: Instruction
    ) {
        @Suppress("UNCHECKED_CAST")
        fun <T> getInstruction(): T = instruction as T
    }
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
     *    opcode(Opcode.MOVE_RESULT_OBJECT, maxAfter = 0), // Must match exactly after INVOKE_VIRTUAL.
     *    opcode(Opcode.IPUT_OBJECT, maxAfter = 0) // Must match exactly after MOVE_RESULT_OBJECT.
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


