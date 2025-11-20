@file:Suppress("unused")

package app.revanced.patcher

import app.revanced.patcher.FieldAccessFilter.Companion.parseJvmFieldAccess
import app.revanced.patcher.MethodCallFilter.Companion.parseJvmMethodCall
import com.android.tools.smali.dexlib2.Opcode
import com.android.tools.smali.dexlib2.iface.Method
import com.android.tools.smali.dexlib2.iface.instruction.Instruction
import com.android.tools.smali.dexlib2.iface.instruction.ReferenceInstruction
import com.android.tools.smali.dexlib2.iface.instruction.WideLiteralInstruction
import com.android.tools.smali.dexlib2.iface.reference.FieldReference
import com.android.tools.smali.dexlib2.iface.reference.MethodReference
import com.android.tools.smali.dexlib2.iface.reference.StringReference
import com.android.tools.smali.dexlib2.iface.reference.TypeReference
import java.util.EnumSet

/**
 * Simple interface to control how much space is allowed between a previous
 * [InstructionFilter match and the current [InstructionFilter].
 */
fun interface InstructionLocation {
    /**
     * @param previouslyMatchedIndex The previously matched index, or -1 if this is the first filter.
     * @param currentIndex The current method index that is about to be checked.
     */
    fun indexIsValidForMatching(previouslyMatchedIndex: Int, currentIndex: Int) : Boolean

    /**
     * Matching can occur anywhere after the previous instruction filter match index.
     * Is the default behavior for all filters.
     */
    class MatchAfterAnywhere : InstructionLocation {
        override fun indexIsValidForMatching(previouslyMatchedIndex: Int, currentIndex: Int) = true
    }

    /**
     * Matches the first instruction of a method.
     *
     * This can only be used for the first filter, and using with any other filter will throw an exception.
     */
    class MatchFirst() : InstructionLocation {
        override fun indexIsValidForMatching(previouslyMatchedIndex: Int, currentIndex: Int) : Boolean {
            require(previouslyMatchedIndex < 0) {
                "MatchFirst can only be used for the first instruction filter"
            }
            return true
        }
    }

    /**
     * Instruction index immediately after the previous filter.
     *
     * Useful for opcodes that must always appear immediately after the last filter such as:
     * - [Opcode.MOVE_RESULT]
     * - [Opcode.MOVE_RESULT_WIDE]
     * - [Opcode.MOVE_RESULT_OBJECT]
     *
     * This cannot be used for the first filter and will throw an exception.
     */
    class MatchAfterImmediately() : InstructionLocation {
        override fun indexIsValidForMatching(previouslyMatchedIndex: Int, currentIndex: Int) : Boolean {
            require(previouslyMatchedIndex >= 0) {
                "MatchAfterImmediately cannot be used for the first instruction filter"
            }
            return currentIndex - 1 == previouslyMatchedIndex
        }
    }

    /**
     * Instruction index can occur within a range of the previous instruction filter match index.
     * used to constrain instruction matching to a region after the previous instruction filter.
     *
     * This cannot be used for the first filter and will throw an exception.
     *
     * @param matchDistance The number of unmatched instructions that can exist between the
     *                      current instruction filter and the previously matched instruction filter.
     *                      A value of 0 means the current filter can only match immediately after
     *                      the previously matched instruction (making this functionally identical to
     *                      [MatchAfterImmediately]). A value of 10 means between 0 and 10 unmatched
     *                      instructions can exist between the previously matched instruction and
     *                      the current instruction filter.
     */
    class MatchAfterWithin(var matchDistance: Int) : InstructionLocation {
        init {
            require(matchDistance >= 0) {
                "matchDistance must be non-negative"
            }
        }

        override fun indexIsValidForMatching(previouslyMatchedIndex: Int, currentIndex: Int) : Boolean {
            require(previouslyMatchedIndex >= 0) {
                "MatchAfterImmediately cannot be used for the first instruction filter"
            }
            return currentIndex - previouslyMatchedIndex - 1 <= matchDistance
        }
    }

    /**
     * Instruction index can occur only after a minimum number of unmatched instructions from the
     * previous instruction match. Or if this is used with the first filter of a fingerprint then
     * this can only match starting from a given instruction index.
     *
     * @param minimumDistanceFromLastInstruction The minimum number of unmatched instructions that
     * must exist between this instruction and the last matched instruction. A value of 0 is
     * functionally identical to [MatchAfterImmediately].
     */
    class MatchAfterAtLeast(var minimumDistanceFromLastInstruction: Int) : InstructionLocation {
        init {
            require(minimumDistanceFromLastInstruction >= 0) {
                "minimumDistanceFromLastInstruction must >= 0"
            }
        }

        override fun indexIsValidForMatching(previouslyMatchedIndex: Int, currentIndex: Int) : Boolean {
            return currentIndex - previouslyMatchedIndex - 1 >= minimumDistanceFromLastInstruction
        }
    }

    /**
     * Functionally combines both [MatchAfterAtLeast] and [MatchAfterWithin] to give a bounded range
     * where the next instruction can match relative to the previous matched instruction.
     *
     * Unlike [MatchAfterImmediately] or [MatchAfterWithin], this can also be used for the first filter
     * to constrain matching to a specific range starting from index 0.
     *
     * @param minimumDistanceFromLastInstruction The minimum number of unmatched instructions that
     *                                           must exist between this instruction and the last matched
     *                                           instruction.
     * @param maximumDistanceFromLastInstruction The maximum number of unmatched instructions
     *                                           that can exist between this instruction and the last
     *                                           matched instruction.
     */
    class MatchAfterRange(
        minimumDistanceFromLastInstruction: Int,
        maximumDistanceFromLastInstruction: Int
    ) : InstructionLocation {

        private val minMatcher = MatchAfterAtLeast(minimumDistanceFromLastInstruction)
        private val maxMatcher = MatchAfterWithin(maximumDistanceFromLastInstruction)

        init {
            require(minimumDistanceFromLastInstruction <= maximumDistanceFromLastInstruction) {
                "minimumDistanceFromLastInstruction must be <= maximumDistanceFromLastInstruction"
            }
        }

        override fun indexIsValidForMatching(previouslyMatchedIndex: Int, currentIndex: Int): Boolean {
            // For the first filter, previouslyMatchedIndex will be -1, and both delegates
            // will correctly enforce their own semantics starting from index 0.
            return minMatcher.indexIsValidForMatching(previouslyMatchedIndex, currentIndex) &&
                    maxMatcher.indexIsValidForMatching(previouslyMatchedIndex, currentIndex)
        }
    }
}



/**
 * String comparison type.
 */
enum class StringComparisonType {
    EQUALS,
    CONTAINS,
    STARTS_WITH,
    ENDS_WITH;

    /**
     * @param targetString The target string to search
     * @param searchString To search for in the target string (or to compare entirely for equality).
     */
    fun compare(targetString: String, searchString: String): Boolean {
        return when (this) {
            EQUALS -> targetString == searchString
            CONTAINS -> targetString.contains(searchString)
            STARTS_WITH -> targetString.startsWith(searchString)
            ENDS_WITH -> targetString.endsWith(searchString)
        }
    }

    /**
     * Throws [IllegalArgumentException] if the class type search string is invalid and can never match.
     */
    internal fun validateSearchStringForClassType(classTypeSearchString: String) {
        when (this) {
            EQUALS -> {
                STARTS_WITH.validateSearchStringForClassType(classTypeSearchString)
                ENDS_WITH.validateSearchStringForClassType(classTypeSearchString)
            }
            CONTAINS -> Unit // Nothing to validate, anything goes.
            STARTS_WITH -> require(classTypeSearchString.startsWith('L')) {
                "Class type does not start with L: $classTypeSearchString"
            }
            ENDS_WITH -> require(classTypeSearchString.endsWith(';')) {
                "Class type does not end with a semicolon: $classTypeSearchString"
            }
        }
    }
}



/**
 * Matches method [Instruction] objects, similar to how [Fingerprint] matches entire fingerprints.
 *
 * The most basic filters match only opcodes and nothing more,
 * and more precise filters can match:
 * - Field references (get/put opcodes) by name/type.
 * - Method calls (invoke_* opcodes) by name/parameter/return type.
 * - Object instantiation for specific class types.
 * - Literal const values.
 */
fun interface InstructionFilter {

    /**
     * The [InstructionLocation] associated with this filter.
     */
    val location: InstructionLocation
        get() = InstructionLocation.MatchAfterAnywhere()

    /**
     * If this filter matches the method instruction.
     *
     * @param enclosingMethod The method of that contains [instruction].
     * @param instruction The instruction to check for a match.
     */
    fun matches(
        enclosingMethod: Method,
        instruction: Instruction
    ): Boolean
}



class AnyInstruction internal constructor(
    private val filters: List<InstructionFilter>,
    override val location: InstructionLocation
) : InstructionFilter {

    override fun matches(
        enclosingMethod: Method,
        instruction: Instruction
    ) : Boolean {
        return filters.any { filter ->
            filter.matches(enclosingMethod, instruction)
        }
    }
}

/**
 * Logical OR operator where the first filter that matches satisfies this filter.
 */
fun anyInstruction(
    vararg filters: InstructionFilter,
    location: InstructionLocation = InstructionLocation.MatchAfterAnywhere()
) = AnyInstruction(filters.asList(), location)



open class OpcodeFilter(
    val opcode: Opcode,
    override val location: InstructionLocation
) : InstructionFilter {

    override fun matches(
        enclosingMethod: Method,
        instruction: Instruction
    ): Boolean {
        return instruction.opcode == opcode
    }
}

/**
 * Single opcode.
 */
fun opcode(
    opcode: Opcode,
    location: InstructionLocation = InstructionLocation.MatchAfterAnywhere()
) = OpcodeFilter(opcode, location)



/**
 * Matches a single instruction from many kinds of opcodes.
 * If matching only a single opcode instead use [OpcodeFilter].
 */
open class OpcodesFilter private constructor(
    val opcodes: EnumSet<Opcode>?,
    override val location: InstructionLocation
) : InstructionFilter {

    protected constructor(
        /**
         * Value of `null` will match any opcode.
         */
        opcodes: List<Opcode>?,
        location: InstructionLocation
    ) : this(if (opcodes == null) null else EnumSet.copyOf(opcodes), location)

    override fun matches(
        enclosingMethod: Method,
        instruction: Instruction
    ): Boolean {
        if (opcodes == null) {
            return true // Match anything.
        }
        return opcodes.contains(instruction.opcode)
    }

    companion object {
        /**
         * First opcode can match anywhere in a method, but all
         * subsequent opcodes must match after the previous opcode.
         *
         * A value of `null` indicates to match any opcode.
         */
        internal fun listOfOpcodes(opcodes: Collection<Opcode?>): List<InstructionFilter> {
            val list = ArrayList<InstructionFilter>(opcodes.size)
            var location: InstructionLocation? = null

            opcodes.forEach { opcode ->
                // First opcode can match anywhere.
                val opcodeLocation = location ?: InstructionLocation.MatchAfterAnywhere()

                list += if (opcode == null) {
                    // Null opcode matches anything.
                    OpcodesFilter(
                        null as List<Opcode>?,
                        opcodeLocation
                    )
                } else {
                    OpcodeFilter(opcode, opcodeLocation)
                }

                if (location == null) {
                    location = InstructionLocation.MatchAfterImmediately()
                }
            }

            return list
        }
    }
}



class LiteralFilter internal constructor(
    var literal: () -> Long,
    opcodes: List<Opcode>? = null,
    location: InstructionLocation
) : OpcodesFilter(opcodes, location) {

    private var literalValue: Long? = null

    override fun matches(
        enclosingMethod: Method,
        instruction: Instruction
    ): Boolean {
        if (!super.matches(enclosingMethod, instruction)) {
            return false
        }

        if (instruction !is WideLiteralInstruction) return false

        if (literalValue == null) {
            literalValue = literal()
        }

        return instruction.wideLiteral == literalValue
    }
}

/**
 * Literal value, such as:
 * `const v1, 0x7f080318`
 *
 * that can be matched using:
 * `LiteralFilter(0x7f080318)`
 * or
 * `LiteralFilter(2131231512)`
 */
fun literal(
    literal: () -> Long,
    opcodes: List<Opcode>? = null,
    location: InstructionLocation = InstructionLocation.MatchAfterAnywhere()
) = LiteralFilter(literal, opcodes, location)

/**
 * Literal value, such as:
 * `const v1, 0x7f080318`
 *
 * that can be matched using:
 * `LiteralFilter(0x7f080318)`
 * or
 * `LiteralFilter(2131231512L)`
 */
fun literal(
    literal: Long,
    opcodes: List<Opcode>? = null,
    location: InstructionLocation = InstructionLocation.MatchAfterAnywhere()
) = LiteralFilter({ literal }, opcodes, location)

/**
 * Integer literal.
 */
fun literal(
    literal: Int,
    opcodes: List<Opcode>? = null,
    location: InstructionLocation = InstructionLocation.MatchAfterAnywhere()
) = LiteralFilter({ literal.toLong() }, opcodes, location)

/**
 * Double literal. Automatically compares hex value as a double.
 */
fun literal(
    literal: Double,
    opcodes: List<Opcode>? = null,
    location: InstructionLocation = InstructionLocation.MatchAfterAnywhere()
) = LiteralFilter({ literal.toRawBits() }, opcodes, location)

/**
 * Floating point literal. Automatically compares hex value as a floating point.
 */
fun literal(
    literal: Float,
    opcodes: List<Opcode>? = null,
    location: InstructionLocation = InstructionLocation.MatchAfterAnywhere()
) = LiteralFilter({ literal.toRawBits().toLong() }, opcodes, location)



class MethodCallFilter internal constructor(
    val definingClass: (() -> String)? = null,
    val name: (() -> String)? = null,
    val parameters: (() -> List<String>)? = null,
    val returnType: (() -> String)? = null,
    opcodes: List<Opcode>? = null,
    location: InstructionLocation
) : OpcodesFilter(opcodes, location) {

    override fun matches(
        enclosingMethod: Method,
        instruction: Instruction
    ): Boolean {
        if (!super.matches(enclosingMethod, instruction)) {
            return false
        }

        val reference = (instruction as? ReferenceInstruction)?.reference as? MethodReference
        if (reference == null) return false

        if (definingClass != null) {
            val referenceClass = reference.definingClass
            val definingClass = definingClass()

            if (!StringComparisonType.ENDS_WITH.compare(referenceClass, definingClass)) {
                // Check if 'this' defining class is used.
                // Would be nice if this also checked all super classes,
                // but doing so requires iteratively checking all superclasses
                // up to the root class since class defs are mere Strings.
                if (!(definingClass == "this" && referenceClass == enclosingMethod.definingClass)) {
                    return false
                } // else, the method call is for 'this' class.
            }
        }
        if (name != null && reference.name != name()) {
            return false
        }
        if (returnType != null && !StringComparisonType.STARTS_WITH.compare(reference.returnType, returnType())) {
            return false
        }
        if (parameters != null && !parametersStartsWith(reference.parameterTypes, parameters())) {
            return false
        }

        return true
    }

    companion object {
        private val regex = Regex("""^(L[^;]+;)->([^(\s]+)\(([^)]*)\)(\[?L[^;]+;|\[?[BCSIJFDZV])${'$'}""")

        internal fun parseJvmMethodCall(
            methodSignature: String,
            opcodes: List<Opcode>? = null,
            location: InstructionLocation = InstructionLocation.MatchAfterAnywhere()
        ): MethodCallFilter {
            val matchResult = regex.matchEntire(methodSignature)
                ?: throw IllegalArgumentException("Invalid method signature: $methodSignature")

            val classDescriptor = matchResult.groupValues[1]
            val methodName = matchResult.groupValues[2]
            val paramDescriptorString = matchResult.groupValues[3]
            val returnDescriptor = matchResult.groupValues[4]

            val paramDescriptors = parseParameterDescriptors(paramDescriptorString)

            return MethodCallFilter(
                { classDescriptor },
                { methodName },
                { paramDescriptors },
                { returnDescriptor },
                opcodes,
                location
            )
        }

        /**
         * Parses a single JVM type descriptor or an array descriptor at the current position.
         * For example: Lcom/example/SomeClass; or I or [I or [Lcom/example/SomeClass;
         */
        private fun parseSingleType(params: String, startIndex: Int): Pair<String, Int> {
            var i = startIndex

            // Skip past array declaration, including multi-dimensional arrays.
            val paramsLength = params.length
            while (i < paramsLength && params[i] == '[') {
                i++
            }

            return if (i < paramsLength && params[i] == 'L') {
                // It's an object type starting with 'L', read until ';'
                val semicolonPos = params.indexOf(';', i)
                if (semicolonPos < 0) {
                    throw IllegalArgumentException("Malformed object descriptor (missing semicolon): $params")
                }
                // Substring from startIndex up to and including the semicolon.
                val typeDescriptor = params.substring(startIndex, semicolonPos + 1)
                typeDescriptor to (semicolonPos + 1)
            } else {
                // It's either a primitive or we've already consumed the array part
                // So just take one character (e.g. 'I', 'Z', 'B', etc.)
                val typeDescriptor = params.substring(startIndex, i + 1)
                typeDescriptor to (i + 1)
            }
        }

        /**
         * Parses the parameters into a list of JVM type descriptors.
         */
        private fun parseParameterDescriptors(paramString: String): List<String> {
            val result = mutableListOf<String>()
            var currentIndex = 0
            val stringLength = paramString.length

            while (currentIndex < stringLength) {
                val (type, nextIndex) = parseSingleType(paramString, currentIndex)
                result.add(type)
                currentIndex = nextIndex
            }

            return result
        }
    }
}

/**
 * Identifies method calls.
 *
 * `Null` parameters matches anything.
 *
 * By default any type of method call matches.
 * Specify opcodes if a specific type of method call is desired (such as only static calls).
 */
fun methodCall(
    /**
     * Defining class of the method call. Matches using endsWith().
     *
     * For calls to a method in the same class, use 'this' as the defining class.
     * Note: 'this' does not work for methods declared only in a superclass.
     */
    definingClass: (() -> String)? = null,
    /**
     * Method name. Must be exact match of the method name.
     */
    name: (() -> String)? = null,
    /**
     * Parameters of the method call. Each parameter matches
     * using startsWith() and semantics are the same as [Fingerprint].
     */
    parameters: (() -> List<String>)? = null,
    /**
     * Return type. Matches using startsWith()
     */
    returnType: (() -> String)? = null,
    /**
     * Opcode types to match. By default this matches any method call opcode:
     * `Opcode.INVOKE_*`.
     *
     * If this filter must match specific types of method call, then specify the desired opcodes
     * such as [Opcode.INVOKE_STATIC], [Opcode.INVOKE_STATIC_RANGE] to match only static calls.
     */
    opcodes: List<Opcode>? = null,
    /**
     * The locations where this filter is allowed to match.
     */
    location: InstructionLocation = InstructionLocation.MatchAfterAnywhere()
) = MethodCallFilter(
    definingClass,
    name,
    parameters,
    returnType,
    opcodes,
    location
)

fun methodCall(
    /**
     * Defining class of the method call. Matches using [StringComparisonType.ENDS_WITH].
     *
     * For calls to a method in the same class, use 'this' as the defining class.
     * Note: 'this' does not work for methods declared only in a superclass.
     */
    definingClass: String? = null,
    /**
     * Method name. Must be exact match of the method name ([StringComparisonType.EQUALS]).
     */
    name: String? = null,
    /**
     * Parameters of the method call. Each parameter matches
     * using startsWith() and semantics are the same as [Fingerprint].
     */
    parameters: List<String>? = null,
    /**
     * Return type. Matches using [StringComparisonType.STARTS_WITH].
     */
    returnType: String? = null,
    /**
     * Opcode types to match. By default this matches any method call opcode:
     * `Opcode.INVOKE_*`.
     *
     * If this filter must match specific types of method call, then specify the desired opcodes
     * such as [Opcode.INVOKE_STATIC], [Opcode.INVOKE_STATIC_RANGE] to match only static calls.
     */
    opcodes: List<Opcode>? = null,
    /**
     * The locations where this filter is allowed to match.
     */
    location: InstructionLocation = InstructionLocation.MatchAfterAnywhere()
) = MethodCallFilter(
    if (definingClass != null) {
        { definingClass }
    } else null,
    if (name != null) {
        { name }
    } else null,
    if (parameters != null) {
        { parameters }
    } else null,
    if (returnType != null) {
        { returnType }
    } else null,
    opcodes,
    location
)

fun methodCall(
    /**
     * Defining class of the method call. Matches using [StringComparisonType.ENDS_WITH].
     *
     * For calls to a method in the same class, use 'this' as the defining class.
     * Note: 'this' does not work for methods declared only in a superclass.
     */
    definingClass: String? = null,
    /**
     * Method name. Must be exact match of the method name ([StringComparisonType.EQUALS]).
     */
    name: String? = null,
    /**
     * Parameters of the method call. Each parameter matches using [StringComparisonType.STARTS_WITH]
     * and semantics are the same as [Fingerprint] parameters.
     */
    parameters: List<String>? = null,
    /**
     * Return type. Matches using [StringComparisonType.STARTS_WITH].
     */
    returnType: String? = null,
    /**
     * Opcode types to match. By default this matches any method call opcode:
     * `Opcode.INVOKE_*`.
     *
     * If this filter must match specific types of method call, then specify the desired opcodes
     * such as [Opcode.INVOKE_STATIC], [Opcode.INVOKE_STATIC_RANGE] to match only static calls.
     */
    opcode: Opcode,
    /**
     * The locations where this filter is allowed to match.
     */
    location: InstructionLocation = InstructionLocation.MatchAfterAnywhere()
) = MethodCallFilter(
    if (definingClass != null) {
        { definingClass }
    } else null,
    if (name != null) {
        { name }
    } else null,
    if (parameters != null) {
        { parameters }
    } else null,
    if (returnType != null) {
        { returnType }
    } else null,
    listOf(opcode),
    location
)

/**
 * Method call for a copy pasted SMALI style method signature. e.g.:
 * `Landroid/view/View;->inflate(Landroid/content/Context;ILandroid/view/ViewGroup;)Landroid/view/View;`
 *
 * Does not support obfuscated method names or parameter/return types.
 */
fun methodCall(
    smali: String,
    opcodes: List<Opcode>? = null,
    location: InstructionLocation = InstructionLocation.MatchAfterAnywhere()
) = parseJvmMethodCall(smali, opcodes, location)

/**
 * Method call for a copy pasted SMALI style method signature. e.g.:
 * `Landroid/view/View;->inflate(Landroid/content/Context;ILandroid/view/ViewGroup;)Landroid/view/View;`
 *
 * Does not support obfuscated method names or parameter/return types.
 */
fun methodCall(
    smali: String,
    opcode: Opcode,
    location: InstructionLocation = InstructionLocation.MatchAfterAnywhere()
) = parseJvmMethodCall(smali, listOf(opcode), location)



class FieldAccessFilter internal constructor(
    val definingClass: (() -> String)? = null,
    val name: (() -> String)? = null,
    val type: (() -> String)? = null,
    opcodes: List<Opcode>? = null,
    location: InstructionLocation
) : OpcodesFilter(opcodes, location) {

    override fun matches(
        enclosingMethod: Method,
        instruction: Instruction
    ): Boolean {
        if (!super.matches(enclosingMethod, instruction)) {
            return false
        }

        val reference = (instruction as? ReferenceInstruction)?.reference as? FieldReference
        if (reference == null) return false

        if (definingClass != null) {
            val referenceClass = reference.definingClass
            val definingClass = definingClass()

            if (!referenceClass.endsWith(definingClass)) {
                if (!(definingClass == "this" && referenceClass == enclosingMethod.definingClass)) {
                    return false
                } // else, the method call is for 'this' class.
            }
        }
        if (name != null && reference.name != name()) {
            return false
        }
        if (type != null && !reference.type.startsWith(type())) {
            return false
        }

        return true
    }

    internal companion object {
        private val regex = Regex("""^(L[^;]+;)->([^:]+):(\[?L[^;]+;|\[?[BCSIJFDZV])${'$'}""")

        internal fun parseJvmFieldAccess(
            fieldSignature: String,
            opcodes: List<Opcode>? = null,
            location: InstructionLocation = InstructionLocation.MatchAfterAnywhere()
        ): FieldAccessFilter {
            val matchResult = regex.matchEntire(fieldSignature)
                ?: throw IllegalArgumentException("Invalid field access smali: $fieldSignature")

            return fieldAccess(
                definingClass = matchResult.groupValues[1],
                name = matchResult.groupValues[2],
                type = matchResult.groupValues[3],
                opcodes = opcodes,
                location = location
            )
        }
    }
}

/**
 * Matches a field call, such as:
 * `iget-object v0, p0, Lahhh;->g:Landroid/view/View;`
 */
fun fieldAccess(
    /**
     * Defining class of the field call. Compares using [StringComparisonType.ENDS_WITH].
     *
     * For calls to a method in the same class, use 'this' as the defining class.
     * Note: 'this' does not work for fields found in superclasses.
     */
    definingClass: (() -> String)? = null,
    /**
     * Name of the field. Must be a full match of the field name.
     */
    name: (() -> String)? = null,
    /**
     * Class type of field. Compares using [StringComparisonType.STARTS_WITH].
     */
    type: (() -> String)? = null,
    /**
     * Valid opcodes matches for this instruction.
     * By default this matches any kind of field access
     * (`Opcode.IGET`, `Opcode.SGET`, `Opcode.IPUT`, `Opcode.SPUT`, etc).
     */
    opcodes: List<Opcode>? = null,
    /**
     * The locations where this filter is allowed to match.
     */
    location: InstructionLocation = InstructionLocation.MatchAfterAnywhere()
)  = FieldAccessFilter(definingClass, name, type, opcodes, location)

/**
 * Matches a field call, such as:
 * `iget-object v0, p0, Lahhh;->g:Landroid/view/View;`
 */
fun fieldAccess(
    /**
     * Defining class of the field call. Compares using [StringComparisonType.ENDS_WITH].
     *
     * For calls to a method in the same class, use 'this' as the defining class.
     * Note: 'this' does not work for fields found in superclasses.
     */
    definingClass: String? = null,
    /**
     * Full field name of the field. Compares using ([StringComparisonType.EQUALS]).
     */
    name: String? = null,
    /**
     * Class type of field. Compares using [StringComparisonType.STARTS_WITH].
     */
    type: String? = null,
    /**
     * Valid opcodes matches for this instruction.
     * By default this matches any kind of field access
     * (`Opcode.IGET`, `Opcode.SGET`, `Opcode.IPUT`, `Opcode.SPUT`, etc).
     */
    opcodes: List<Opcode>? = null,
    /**
     * The locations where this filter is allowed to match.
     */
    location: InstructionLocation = InstructionLocation.MatchAfterAnywhere()
) = FieldAccessFilter(
    if (definingClass != null) {
        { definingClass }
    } else null,
    if (name != null) {
        { name }
    } else null,
    if (type != null) {
        { type }
    } else null,
    opcodes,
    location
)

/**
 * Matches a field call, such as:
 * `iget-object v0, p0, Lahhh;->g:Landroid/view/View;`
 */
fun fieldAccess(
    /**
     * Defining class of the field call. Compares using [StringComparisonType.ENDS_WITH].
     *
     * For calls to a method in the same class, use 'this' as the defining class.
     * Note: 'this' does not work for fields found in superclasses.
     */
    definingClass: String? = null,
    /**
     * Full name of the field. Compares using [StringComparisonType.EQUALS].
     */
    name: String? = null,
    /**
     * Class type of field. Compares using [StringComparisonType.STARTS_WITH].
     */
    type: String? = null,
    opcode: Opcode,
    /**
     * The locations where this filter is allowed to match.
     */
    location: InstructionLocation = InstructionLocation.MatchAfterAnywhere()
) = fieldAccess(
    definingClass,
    name,
    type,
    listOf(opcode),
    location
)

/**
 * Field access for a copy pasted SMALI style field access call. e.g.:
 * `Ljava/lang/Boolean;->TRUE:Ljava/lang/Boolean;`
 *
 * Should never be used with obfuscated field names or obfuscated field types.
 */
fun fieldAccess(
    smali: String,
    opcodes: List<Opcode>? = null,
    location: InstructionLocation = InstructionLocation.MatchAfterAnywhere()
) = parseJvmFieldAccess(smali, opcodes, location)

/**
 * Field access for a copy pasted SMALI style field access call. e.g.:
 * `Ljava/lang/Boolean;->TRUE:Ljava/lang/Boolean;`
 *
 * Should never be used with obfuscated field names or obfuscated field types.
 */
fun fieldAccess(
    smali: String,
    opcode: Opcode,
    location: InstructionLocation = InstructionLocation.MatchAfterAnywhere()
) = parseJvmFieldAccess(smali, listOf(opcode), location)



class StringFilter internal constructor(
    var string: () -> String,
    var comparisonType: StringComparisonType,
    location: InstructionLocation
) : OpcodesFilter(listOf(Opcode.CONST_STRING, Opcode.CONST_STRING_JUMBO), location) {

    override fun matches(
        enclosingMethod: Method,
        instruction: Instruction
    ): Boolean {
        if (!super.matches(enclosingMethod, instruction)) {
            return false
        }

        val instructionString = ((instruction as ReferenceInstruction).reference as StringReference).string
        val filterString = string()

        return comparisonType.compare(instructionString, filterString);
    }
}

/**
 * Literal String instruction.
 */
fun string(
    string: () -> String,
    /**
     * For partial matching of a string opcode and defaults to full string equality. For more
     * precise matching of multiple strings, consider using [anyInstruction] with multiple
     * exact string declarations.
     */
    matchType: StringComparisonType = StringComparisonType.EQUALS,
    location: InstructionLocation = InstructionLocation.MatchAfterAnywhere()
) = StringFilter(string, matchType, location)

/**
 * Literal String instruction.
 */
fun string(
    string: String,
    /**
     * For partial matching of a string opcode and defaults to full string equality. For more
     * precise matching of multiple strings, consider using [anyInstruction] with multiple
     * exact string declarations.
     */
    matchType: StringComparisonType = StringComparisonType.EQUALS,
    location: InstructionLocation = InstructionLocation.MatchAfterAnywhere()
) = StringFilter({ string }, matchType, location)



class NewInstanceFilter internal constructor (
    var type: () -> String,
    var stringComparison: StringComparisonType,
    location: InstructionLocation
) : OpcodesFilter(listOf(Opcode.NEW_INSTANCE, Opcode.NEW_ARRAY), location) {

    override fun matches(
        enclosingMethod: Method,
        instruction: Instruction
    ): Boolean {
        if (!super.matches(enclosingMethod, instruction)) {
            return false
        }

        val reference = (instruction as? ReferenceInstruction)?.reference as? TypeReference
        if (reference == null) return false
        val referenceType = reference.type
        val classType = type()

        stringComparison.validateSearchStringForClassType(classType)
        return stringComparison.compare(referenceType, classType)
    }
}


/**
 * Opcode type [Opcode.NEW_INSTANCE] or [Opcode.NEW_ARRAY] with a non obfuscated class type.
 *
 * @param type Class type.
 * @param stringComparison How to compare the opcode class type. Defaults to [StringComparisonType.ENDS_WITH].
 */
fun newInstancetype(
    type: () -> String,
    location: InstructionLocation = InstructionLocation.MatchAfterAnywhere(),
    stringComparison: StringComparisonType = StringComparisonType.ENDS_WITH
) = NewInstanceFilter(type, stringComparison, location)

/**
 * Opcode type [Opcode.NEW_INSTANCE] or [Opcode.NEW_ARRAY] with a non obfuscated class type.
 *
 * @param type Class type.
 * @param stringComparison How to compare the opcode class type. Defaults to [StringComparisonType.ENDS_WITH].
 */
fun newInstance(
    type: String,
    location: InstructionLocation = InstructionLocation.MatchAfterAnywhere(),
    stringComparison: StringComparisonType = StringComparisonType.ENDS_WITH
) = NewInstanceFilter({ type }, stringComparison, location)



class CheckCastFilter internal constructor (
    var type: () -> String,
    var stringComparison: StringComparisonType,
    location: InstructionLocation = InstructionLocation.MatchAfterAnywhere()
) : OpcodeFilter(Opcode.CHECK_CAST, location) {

    override fun matches(
        enclosingMethod: Method,
        instruction: Instruction
    ): Boolean {
        if (!super.matches(enclosingMethod, instruction)) {
            return false
        }

        val reference = (instruction as? ReferenceInstruction)?.reference as? TypeReference
        if (reference == null) return false
        val referenceType = reference.type
        val classType = type()

        stringComparison.validateSearchStringForClassType(classType)
        return stringComparison.compare(referenceType, classType)
    }
}

/**
 * Opcode type [Opcode.CHECK_CAST] with a non obfuscated class type.
 *
 * @param type Class type.
 * @param stringComparison How to compare the opcode class type. Defaults to [StringComparisonType.ENDS_WITH].
 */
fun checkCast(
    type: () -> String,
    stringComparison: StringComparisonType = StringComparisonType.ENDS_WITH,
    location: InstructionLocation = InstructionLocation.MatchAfterAnywhere()
) = CheckCastFilter(type, stringComparison, location)

/**
 * Opcode type [Opcode.CHECK_CAST] with a non obfuscated class type.
 *
 * @param type Class type.
 * @param stringComparison How to compare the opcode class type. Defaults to [StringComparisonType.ENDS_WITH].
 */
fun checkCast(
    type: String,
    stringComparison: StringComparisonType = StringComparisonType.ENDS_WITH,
    location: InstructionLocation = InstructionLocation.MatchAfterAnywhere()
) = CheckCastFilter({ type }, stringComparison, location)
