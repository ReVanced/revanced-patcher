@file:Suppress("unused", "MemberVisibilityCanBePrivate")

package app.revanced.patcher

import app.revanced.patcher.Match.PatternMatch
import app.revanced.patcher.extensions.InstructionExtensions.instructionsOrNull
import app.revanced.patcher.patch.*
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
import kotlin.reflect.KProperty

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
 * @param name Human readable name used for [toString].
 * @param accessFlags The exact access flags using values of [AccessFlags].
 * @param returnType The return type. Compared using [String.startsWith].
 * @param parameters The parameters. Partial matches allowed and follow the same rules as [returnType].
 * @param filters A list of filters to match.
 * @param strings A list of the strings. Compared using [String.contains].
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
     * The match for this [Fingerprint]. Null if unmatched.
     */
    context(BytecodePatchContext)
    private val matchOrNull: Match?
        get() = matchOrNull()

    /**
     * Match using [BytecodePatchContext.lookupMaps].
     *
     * Generally faster than the other [matchOrNull] overloads when there are many methods to check for a match.
     *
     * Fingerprints can be optimized for performance:
     * - Slowest: Specify [custom] or [opcodes] and nothing else.
     * - Fast: Specify [accessFlags], [returnType].
     * - Faster: Specify [accessFlags], [returnType] and [parameters].
     * - Fastest: Specify [strings], with at least one string being an exact (non-partial) match.
     *
     * @return The [Match] if a match was found or if the fingerprint is already matched to a method, null otherwise.
     */
    context(BytecodePatchContext)
    internal fun matchOrNull(): Match? {
        if (_matchOrNull != null) return _matchOrNull

        var match = strings?.mapNotNull {
            lookupMaps.methodsByStrings[it]
        }?.minByOrNull { it.size }?.let { methodClasses ->
            methodClasses.forEach { (classDef, method) ->
                val match = matchOrNull(classDef, method)
                if (match != null) return@let match
            }

            null
        }
        if (match != null) return match

        classes.forEach { classDef ->
            match = matchOrNull(classDef)
            if (match != null) return match
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
            val match = matchOrNull(method, classDef)
            if (match != null) return match
        }

        return null
    }

    /**
     * Match using a [Method].
     * The class is retrieved from the method.
     *
     * @param method The method to match against.
     * @return The [Match] if a match was found or if the fingerprint is already matched to a method, null otherwise.
     */
    context(BytecodePatchContext)
    fun matchOrNull(
        method: Method,
    ) = matchOrNull(method, classBy { method.definingClass == it.type }!!.immutableClass)

    /**
     * Match using a [Method].
     *
     * @param method The method to match against.
     * @param classDef The class the method is a member of.
     * @return The [Match] if a match was found or if the fingerprint is already matched to a method, null otherwise.
     */
    context(BytecodePatchContext)
    fun matchOrNull(
        method: Method,
        classDef: ClassDef,
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

        val stringMatches: List<Match.StringMatch>? =
            if (strings != null) {
                buildList {
                    val instructions = method.instructionsOrNull ?: return null

                    val stringsList = strings.toMutableList()

                    instructions.forEachIndexed { instructionIndex, instruction ->
                        if (
                            instruction.opcode != Opcode.CONST_STRING &&
                            instruction.opcode != Opcode.CONST_STRING_JUMBO
                        ) {
                            return@forEachIndexed
                        }

                        val string = ((instruction as ReferenceInstruction).reference as StringReference).string
                        val index = stringsList.indexOfFirst(string::contains)
                        if (index == -1) return@forEachIndexed

                        add(Match.StringMatch(string, instructionIndex))
                        stringsList.removeAt(index)
                    }

                    if (stringsList.isNotEmpty()) return null
                }
            } else {
                null
            }

        val filterMatch = if (filters != null) {
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
        } else {
            null
        }

        _matchOrNull = Match(
            method,
            filterMatch,
            stringMatches,
            classDef,
        )

        return _matchOrNull
    }

    private val exception get() = PatchException("Failed to match the fingerprint: $this")

    override fun toString() = name

    /**
     * The match for this [Fingerprint].
     *
     * @throws PatchException If the [Fingerprint] has not been matched.
     */
    context(BytecodePatchContext)
    private val match
        get() = matchOrNull ?: throw exception

    /**
     * Match using a [ClassDef].
     *
     * @param classDef The class to match against.
     * @return The [Match] if a match was found or if the fingerprint is already matched to a method, null otherwise.
     * @throws PatchException If the fingerprint has not been matched.
     */
    context(BytecodePatchContext)
    fun match(
        classDef: ClassDef,
    ) = matchOrNull(classDef) ?: throw exception

    /**
     * Match using a [Method].
     * The class is retrieved from the method.
     *
     * @param method The method to match against.
     * @return The [Match] if a match was found or if the fingerprint is already matched to a method, null otherwise.
     * @throws PatchException If the fingerprint has not been matched.
     */
    context(BytecodePatchContext)
    fun match(
        method: Method,
    ) = matchOrNull(method) ?: throw exception

    /**
     * Match using a [Method].
     *
     * @param method The method to match against.
     * @param classDef The class the method is a member of.
     * @return The [Match] if a match was found or if the fingerprint is already matched to a method, null otherwise.
     * @throws PatchException If the fingerprint has not been matched.
     */
    context(BytecodePatchContext)
    fun match(
        method: Method,
        classDef: ClassDef,
    ) = matchOrNull(method, classDef) ?: throw exception

    /**
     * The class the matching method is a member of.
     */
    context(BytecodePatchContext)
    val originalClassDefOrNull
        get() = matchOrNull?.originalClassDef

    /**
     * The matching method.
     */
    context(BytecodePatchContext)
    val originalMethodOrNull
        get() = matchOrNull?.originalMethod

    /**
     * The mutable version of [originalClassDefOrNull].
     *
     * Accessing this property allocates a [ClassProxy].
     * Use [originalClassDefOrNull] if mutable access is not required.
     */
    context(BytecodePatchContext)
    val classDefOrNull
        get() = matchOrNull?.classDef

    /**
     * The mutable version of [originalMethodOrNull].
     *
     * Accessing this property allocates a [ClassProxy].
     * Use [originalMethodOrNull] if mutable access is not required.
     */
    context(BytecodePatchContext)
    val methodOrNull
        get() = matchOrNull?.method

    /**
     * The match for the opcode pattern.
     */
    context(BytecodePatchContext)
    val patternMatchOrNull : PatternMatch?
        get() {
            if (matchOrNull == null || matchOrNull!!.filterMatches == null) {
                return null
            }
            return matchOrNull!!.patternMatch
        }

    /**
     * The match for the instruction filters.
     */
    context(BytecodePatchContext)
    val filterMatchOrNull
        get() = matchOrNull?.filterMatches

    /**
     * The matches for the strings.
     */
    context(BytecodePatchContext)
    val stringMatchesOrNull
        get() = matchOrNull?.stringMatches

    /**
     * The class the matching method is a member of.
     *
     * @throws PatchException If the fingerprint has not been matched.
     */
    context(BytecodePatchContext)
    val originalClassDef
        get() = match.originalClassDef

    /**
     * The matching method.
     *
     * @throws PatchException If the fingerprint has not been matched.
     */
    context(BytecodePatchContext)
    val originalMethod
        get() = match.originalMethod

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
        get() = match.classDef

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
        get() = match.method

    /**
     * The match for the opcode pattern.
     *
     * @throws PatchException If the fingerprint has not been matched.
     */
    context(BytecodePatchContext)
    @Deprecated("Instead use filterMatch")
    val patternMatch
        get() = match.patternMatch

    /**
     * Instruction filter matches.
     *
     * @throws PatchException If the fingerprint has not been matched.
     */
    context(BytecodePatchContext)
    val filterMatch
        get() = match.filterMatches ?: throw PatchException("Did not match $this")

    /**
     * The matches for the strings.
     *
     * @throws PatchException If the fingerprint has not been matched.
     */
    context(BytecodePatchContext)
    val stringMatches
        get() = match.stringMatches ?: throw PatchException("Did not match $this")
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
    val originalMethod: Method,
    val filterMatches: List<FilterMatch>?,
    val stringMatches: List<StringMatch>?,
    val originalClassDef: ClassDef,
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

    @Deprecated("Instead use filterMatch")
    val patternMatch by lazy {
        if (filterMatches == null) throw PatchException("Did not match $this")
        PatternMatch(filterMatches!!.first().index, filterMatches.last().index)
    }

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
    class FilterMatch(
        val filter : InstructionFilter,
        val index: Int,
        val instruction: Instruction
    )

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
     * Set the opcodes.
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
    // Called when you read the property, e.g. `val x by fingerprint { ... }`
    operator fun getValue(thisRef: Any?, property: KProperty<*>): Fingerprint {
        val name = property.name
        val builder = FingerprintBuilder(name)
        builder.block() // Apply the DSL block.
        return builder.build()
    }
}

fun fingerprint(block: FingerprintBuilder.() -> Unit): FingerprintDelegate {
    return FingerprintDelegate(block)
}