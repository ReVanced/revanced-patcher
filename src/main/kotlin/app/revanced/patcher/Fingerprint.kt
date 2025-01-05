@file:Suppress("unused", "MemberVisibilityCanBePrivate")

package app.revanced.patcher

import app.revanced.patcher.Match.PatternMatch
import app.revanced.patcher.extensions.InstructionExtensions.instructionsOrNull
import app.revanced.patcher.patch.*
import app.revanced.patcher.util.parametersStartsWith
import app.revanced.patcher.util.proxy.ClassProxy
import com.android.tools.smali.dexlib2.AccessFlags
import com.android.tools.smali.dexlib2.Opcode
import com.android.tools.smali.dexlib2.iface.ClassDef
import com.android.tools.smali.dexlib2.iface.Method
import com.android.tools.smali.dexlib2.iface.instruction.Instruction
import com.android.tools.smali.dexlib2.iface.instruction.ReferenceInstruction
import com.android.tools.smali.dexlib2.iface.reference.StringReference
import com.android.tools.smali.dexlib2.util.MethodUtil
import kotlin.collections.forEach
import kotlin.lazy
import kotlin.reflect.KProperty

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
 * @param name Human readable fingerprint name used for [toString] and error stack traces.
 * @param accessFlags The exact access flags using values of [AccessFlags].
 * @param returnType The return type. Compared using [String.startsWith].
 * @param parameters The parameters. Partial matches allowed and follow the same rules as [returnType].
 * @param filters A list of filters to match, declared in the same order the instructions appear in the method.
 * @param strings A list of the strings that appear anywhere in the method. Compared using [String.contains].
 * @param custom A custom condition for this fingerprint.
 */
class Fingerprint internal constructor(
    internal val name: String,
    internal val accessFlags: Int?,
    internal val returnType: String?,
    internal val parameters: List<String>?,
    internal val filters: List<InstructionFilter>?,
    internal val strings: List<String>?,
    internal val custom: ((method: Method, classDef: ClassDef) -> Boolean)?,
) {
    @Suppress("ktlint:standard:backing-property-naming")
    // Backing field needed for lazy initialization.
    private var _matchOrNull: Match? = null

    /**
     * The match for this [Fingerprint], or `null` if no matches exist.
     */
    context(BytecodePatchContext)
    fun matchOrNull(): Match? {
        if (_matchOrNull != null) return _matchOrNull

        strings?.mapNotNull {
            lookupMaps.methodsByStrings[it]
        }?.minByOrNull { it.size }?.let { methodClasses ->
            methodClasses.forEach { (method, classDef) ->
                val match = matchOrNull(classDef, method)
                if (match != null) {
                    _matchOrNull = match
                    return match
                }
            }
        }

        classes.forEach { classDef ->
            val match = matchOrNull(classDef)
            if (match != null) {
                _matchOrNull = match
                return match
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

        return matchOrNull(classBy { method.definingClass == it.type }!!.immutableClass, method)
    }

    /**
     * Match using a [Method].
     *
     * @param method The method to match against.
     * @param classDef The class the method is a member of.
     * @return The [Match] if a match was found or if the fingerprint is previously matched to a method,
     * otherwise `null`.
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
                    if (index == -1) return@forEachIndexed

                    add(Match.StringMatch(string, instructionIndex))
                    stringsList.removeAt(index)
                }

                if (stringsList == null || stringsList.isNotEmpty()) return null
            }
        }

        val filterMatch = if (filters == null) {
            null
        } else {
            val instructions = method.instructionsOrNull?.toList() ?: return null

            fun matchFilters() : List<Match.FilterMatch>? {
                val lastMethodIndex = instructions.lastIndex
                var filterMatches : MutableList<Match.FilterMatch>? = null

                var firstInstructionIndex = 0
                firstFilterLoop@ while (true) {
                    // Matched index of the first filter.
                    var firstFilterIndex = -1
                    var subIndex = firstInstructionIndex

                    for (filterIndex in filters.indices) {
                        val filter = filters[filterIndex]
                        val maxIndex = (subIndex + filter.maxInstructionsBefore)
                            .coerceAtMost(lastMethodIndex)
                        var filterMatched = false

                        while (subIndex <= maxIndex) {
                            val instruction = instructions[subIndex]
                            if (filter.matches(this@BytecodePatchContext, method, instruction, subIndex)) {
                                if (filterIndex == 0) {
                                    firstFilterIndex = subIndex
                                }
                                if (filterMatches == null) {
                                    filterMatches = ArrayList<Match.FilterMatch>(filters.size)
                                }
                                filterMatches += Match.FilterMatch(filter, subIndex, instruction)
                                filterMatched = true
                                subIndex++
                                break
                            }
                            subIndex++
                        }

                        if (!filterMatched) {
                            if (filterIndex == 0) {
                                return null // First filter has no more matches to start from.
                            }

                            // Try again with the first filter, starting from
                            // the next possible first filter index.
                            firstInstructionIndex = firstFilterIndex + 1
                            filterMatches?.clear()
                            continue@firstFilterLoop
                        }
                    }

                    // All instruction filters matches.
                    return filterMatches
                }
            }

            matchFilters() ?: return null
        }

        _matchOrNull = Match(
            classDef,
            method,
            filterMatch,
            stringMatches,
        )

        return _matchOrNull
    }

    fun patchException() = PatchException("Failed to match the fingerprint: $this")

    override fun toString() = name


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
     * Accessing this property allocates a [ClassProxy].
     * Use [originalClassDefOrNull] if mutable access is not required.
     */
    context(BytecodePatchContext)
    val classDefOrNull
        get() = matchOrNull()?.classDef

    /**
     * The mutable version of [originalMethodOrNull].
     *
     * Accessing this property allocates a [ClassProxy].
     * Use [originalMethodOrNull] if mutable access is not required.
     */
    context(BytecodePatchContext)
    val methodOrNull
        get() = matchOrNull()?.method

    /**
     * The match for the opcode pattern, or null if this fingerprint did not match.
     */
    context(BytecodePatchContext)
    @Deprecated("instead use filterMatchesOrNull")
    val patternMatchOrNull : PatternMatch?
        get() {
            val match = this.matchOrNull()
            if (match == null || match.filterMatchesOrNull == null) {
                return null
            }
            return match.patternMatch
        }

    /**
     * The match for the instruction filters, or null if this fingerprint did not match.
     */
    context(BytecodePatchContext)
    val filterMatchesOrNull
        get() = matchOrNull()?.filterMatchesOrNull

    /**
     * The matches for the strings, or null if this fingerprint did not match.
     */
    context(BytecodePatchContext)
    val stringMatchesOrNull
        get() = matchOrNull()?.stringMatchesOrNull

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
     * Accessing this property allocates a [ClassProxy].
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
     * Accessing this property allocates a [ClassProxy].
     * Use [originalMethod] if mutable access is not required.
     *
     * @throws PatchException If the fingerprint has not been matched.
     */
    context(BytecodePatchContext)
    val method
        get() = match().method

    /**
     * The match for the opcode pattern.
     *
     * @throws PatchException If the fingerprint has not been matched.
     */
    context(BytecodePatchContext)
    @Deprecated("Instead use filterMatch")
    val patternMatch
        get() = match().patternMatch

    /**
     * Instruction filter matches.
     *
     * @throws PatchException If the fingerprint has not been matched.
     */
    context(BytecodePatchContext)
    val filterMatches
        get() = match().filterMatches

    /**
     * The matches for the strings.
     *
     * @throws PatchException If the fingerprint has not been matched.
     */
    context(BytecodePatchContext)
    val stringMatches
        get() = match().stringMatches
}

/**
 * A match of a [Fingerprint].
 *
 * @param originalClassDef The class the matching method is a member of.
 * @param originalMethod The matching method.
 * @param patternMatch The match for the opcode pattern.
 * @param stringMatches The matches for the strings.
 */
context(BytecodePatchContext)
class Match internal constructor(
    val originalClassDef: ClassDef,
    val originalMethod: Method,
    private val _filterMatches: List<FilterMatch>?,
    private val _stringMatches: List<StringMatch>?,
) {
    /**
     * The mutable version of [originalClassDef].
     *
     * Accessing this property allocates a [ClassProxy].
     * Use [originalClassDef] if mutable access is not required.
     */
    val classDef by lazy { proxy(originalClassDef).mutableClass }

    /**
     * The mutable version of [originalMethod].
     *
     * Accessing this property allocates a [ClassProxy].
     * Use [originalMethod] if mutable access is not required.
     */
    val method by lazy { classDef.methods.first { MethodUtil.methodSignaturesMatch(it, originalMethod) } }

    @Deprecated("Instead use filterMatches", ReplaceWith("filterMatches"))
    val patternMatch by lazy {
        if (_filterMatches == null) throw PatchException("Did not match $this")
        @SuppressWarnings("deprecation")
        PatternMatch(_filterMatches.first().index, _filterMatches.last().index)
    }

    val filterMatches
        get() = _filterMatches ?: throw PatchException("Fingerprint declared no filters")
    val filterMatchesOrNull = _filterMatches

    val stringMatches
        get() = _stringMatches ?: throw PatchException("Fingerprint declared no strings")
    val stringMatchesOrNull = _stringMatches

    /**
     * A match for an opcode pattern.
     * @param startIndex The index of the first opcode of the pattern in the method.
     * @param endIndex The index of the last opcode of the pattern in the method.
     */
    @Deprecated("Instead use FilterMatch")
    class PatternMatch internal constructor(
        val startIndex: Int,
        val endIndex: Int,
    )

    /**
     * A match for a [InstructionFilter].
     * @param filter The filter that matched
     * @param index The instruction index it matched with.
     * @param instruction The instruction that matched.
     */
    class FilterMatch internal constructor(
        val filter : InstructionFilter,
        val index: Int,
        val instruction: Instruction
    ) {
        @Suppress("UNCHECKED_CAST")
        fun <T> getInstruction(): T = instruction as T
    }

    /**
     * A match for a string.
     *
     * @param string The string that matched.
     * @param index The index of the instruction in the method.
     */
    class StringMatch internal constructor(val string: String, val index: Int)
}

/**
 * A builder for [Fingerprint].
 *
 * @property name Name of the fingerprint, and usually identical to the variable name.
 * @property accessFlags The exact access flags using values of [AccessFlags].
 * @property returnType The return type compared using [String.startsWith].
 * @property parameters The parameters of the method. Partial matches allowed and follow the same rules as [returnType].
 * @property instructionFilters Filters to match the method instructions.
 * @property strings A list of the strings compared each using [String.contains].
 * @property customBlock A custom condition for this fingerprint.
 *
 * @constructor Create a new [FingerprintBuilder].
 */
class FingerprintBuilder(val name: String) {
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
     * @param returnType The return type compared using [String.startsWith].
     */
    fun returns(returnType: String) {
        this.returnType = returnType
    }

    /**
     * Set the parameters.
     *
     * @param parameters The parameters of the method. Partial matches allowed and follow the same rules as [returnType].
     */
    fun parameters(vararg parameters: String) {
        this.parameters = parameters.toList()
    }

    private fun verifyNoFiltersSet() {
        if (this.instructionFilters != null) {
            throw PatchException("Instruction filters already set.")
        }
    }

    /**
     * A pattern of opcodes.
     *
     * To use opcodes with other [InstructionFilter] objects, instead use
     * [instructions] with individual opcodes declared using [OpcodeFilter].
     *
     * @param opcodes An opcode pattern of instructions.
     * Wildcard or unknown opcodes can be specified by `null`.
     */
    fun opcodes(vararg opcodes: Opcode?) {
        verifyNoFiltersSet()
        this.instructionFilters = OpcodeFilter.listOfOpcodes(opcodes.toList())
    }

    /**
     * Set the opcodes.
     *
     * @param opcodes An opcode pattern of instructions.
     * Wildcard or unknown opcodes can be specified by `null`.
     */
    fun instructions(vararg instructionFilters: InstructionFilter) {
        verifyNoFiltersSet()
        this.instructionFilters = instructionFilters.toList()
    }

    /**
     * Set the opcodes.
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
        this.instructionFilters = OpcodeFilter.listOfOpcodes(
            instructions.trimIndent().split("\n").filter {
                it.isNotBlank()
            }.map {
                // Remove any operands.
                val name = it.split(" ", limit = 1).first().trim()
                if (name == "null") return@map null

                opcodesByName[name] ?: throw Exception("Unknown opcode: $name")
            }
        )
    }

    /**
     * Set the strings.
     *
     * @param strings A list of strings compared each using [String.contains].
     */
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

    fun build() = Fingerprint(
        name,
        accessFlags,
        returnType,
        parameters,
        instructionFilters,
        strings,
        customBlock,
    )

    private companion object {
        val opcodesByName = Opcode.entries.associateBy { it.name }
    }
}

class FingerprintDelegate(
    private val block: FingerprintBuilder.() -> Unit
) {
    // Must cache the fingerprint, otherwise on every usage
    // a new fingerprint is built and resolved.
    private var fingerprint: Fingerprint? = null

    private fun getFingerprint(name: String) : Fingerprint {
        if (fingerprint == null) {
            val builder = FingerprintBuilder(name)
            builder.block()
            fingerprint = builder.build()
        }

        return fingerprint!!
    }

    // Called when you read the property, e.g. `val x by fingerprint { ... }`
    operator fun getValue(thisRef: Any?, property: KProperty<*>): Fingerprint {
        return getFingerprint(property.name)
    }
}

/**
 * Create a [Fingerprint].
 *
 * @param block The block to build the [Fingerprint].
 *
 * @return The created [Fingerprint].
 */
fun fingerprint(block: FingerprintBuilder.() -> Unit): FingerprintDelegate {
    return FingerprintDelegate(block)
}