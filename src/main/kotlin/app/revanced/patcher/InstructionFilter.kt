@file:Suppress("unused")

package app.revanced.patcher

import app.revanced.patcher.extensions.InstructionExtensions.instructions
import app.revanced.patcher.patch.BytecodePatchContext
import app.revanced.patcher.util.parametersStartsWith
import com.android.tools.smali.dexlib2.Opcode
import com.android.tools.smali.dexlib2.iface.Method
import com.android.tools.smali.dexlib2.iface.instruction.Instruction
import com.android.tools.smali.dexlib2.iface.instruction.ReferenceInstruction
import com.android.tools.smali.dexlib2.iface.instruction.WideLiteralInstruction
import com.android.tools.smali.dexlib2.iface.reference.FieldReference
import com.android.tools.smali.dexlib2.iface.reference.MethodReference
import com.android.tools.smali.dexlib2.iface.reference.TypeReference
import java.util.EnumSet
import kotlin.collections.forEach

/**
 * Matches method [Instruction] objects, similar to how [Fingerprint] matches entire fingerprints.
 *
 * The most basic filters match only opcodes and nothing more,
 * and more precise filters can match:
 * - Field references (get/put opcodes) by name/type.
 * - Method calls (invoke_* opcodes) by name/parameter/return type.
 * - Object instantiation for specific class types.
 * - Literal const values.
 *
 * Variable space is allowed between each filter.
 *
 * By default [OpcodeFilter] and [OpcodesFilter] use a default [maxInstructionsBefore] of zero,
 * meaning the opcode must be immediately after the previous filter.
 *
 * All other filters use a default [maxInstructionsBefore] of [METHOD_MAX_INSTRUCTIONS] meaning
 * they can match anywhere after the previous filter.
 */
abstract class InstructionFilter(
    /**
     * Maximum number of non matching method instructions that can appear before this filter.
     * A value of zero means this filter must match immediately after the prior filter,
     * or if this is the first filter then this may only match the first instruction of a method.
     */
    val maxInstructionsBefore: Int = METHOD_MAX_INSTRUCTIONS
) {

    /**
     * If this filter matches the method instruction.
     *
     * method index can be ignored unless a filter has an unusual reason,
     * such as checking for the last index of a method.
     */
    abstract fun matches(
        context: BytecodePatchContext,
        method: Method,
        instruction: Instruction,
        methodIndex: Int
    ): Boolean

    companion object {
        /**
         * Maximum number of instructions allowed in a Java method.
         * Indicates to allow a match anywhere after the previous filter.
         */
        const val METHOD_MAX_INSTRUCTIONS = 65535
    }
}

/**
 * Logical or operator, where the first filter that matches is the match result.
 */
class AnyFilter(
    private val filters: List<InstructionFilter>,
    maxInstructionsBefore: Int = METHOD_MAX_INSTRUCTIONS,
) : InstructionFilter(maxInstructionsBefore) {

    override fun matches(
        context: BytecodePatchContext,
        method: Method,
        instruction: Instruction,
        methodIndex: Int
    ): Boolean {
        return filters.any { matches(context, method, instruction, methodIndex) }
    }
}


/**
 * Single opcode.
 */
class OpcodeFilter(
    val opcode: Opcode,
    maxInstructionsBefore: Int = METHOD_MAX_INSTRUCTIONS,
) : InstructionFilter(maxInstructionsBefore) {

    override fun matches(
        context: BytecodePatchContext,
        method: Method,
        instruction: Instruction,
        methodIndex: Int
    ): Boolean {
        return instruction.opcode == opcode
    }

    companion object {
        /**
         * First opcode can match anywhere in a method, but all
         * subsequent opcodes must match after the previous opcode.
         *
         * A value of `null` indicates to match any opcode.
         */
        fun listOfOpcodes(opcodes: Collection<Opcode?>): List<InstructionFilter> {
            var list = ArrayList<InstructionFilter>(opcodes.size)

            // First opcode can match anywhere.
            var instructionsBefore = METHOD_MAX_INSTRUCTIONS
            opcodes.forEach { opcode ->
                list += if (opcode == null) {
                    // Null opcode matches anything.
                    OpcodesFilter(null as List<Opcode>?, instructionsBefore)
                } else {
                    OpcodeFilter(opcode, instructionsBefore)
                }
                instructionsBefore = 0
            }

            return list
        }
    }
}


/**
 * Matches a single instruction from many kinds of opcodes.
 * If matching only a single opcode instead use [OpcodeFilter].
 */
open class OpcodesFilter private constructor(
    val opcodes: EnumSet<Opcode>?,
    maxInstructionsBefore: Int,
) : InstructionFilter(maxInstructionsBefore) {

    constructor(
        /**
         * Value of `null` will match any opcode.
         */
        opcodes: List<Opcode>?,
        maxInstructionsBefore: Int = METHOD_MAX_INSTRUCTIONS
    ) : this(if (opcodes == null) null else EnumSet.copyOf(opcodes), maxInstructionsBefore)

    override fun matches(
        context: BytecodePatchContext,
        method: Method,
        instruction: Instruction,
        methodIndex: Int
    ): Boolean {
        if (opcodes == null) {
            return true // Match anything.
        }
        return opcodes.contains(instruction.opcode)
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
 *
 * Use a lambda if the literal is not known at the declaration of this object such as:
 * `LiteralFilter({ OtherClass.findLiteralToUse() })`
 */
class LiteralFilter(
    var literal: () -> Long,
    opcodes: List<Opcode>? = null,
    maxInstructionsBefore: Int = METHOD_MAX_INSTRUCTIONS,
) : OpcodesFilter(opcodes, maxInstructionsBefore) {

    /**
     * Integer/Long literal.
     */
    constructor(
        literal : Long,
        opcodes: List<Opcode>? = null,
        maxInstructionsBefore: Int = METHOD_MAX_INSTRUCTIONS,
    ) : this({ literal }, opcodes, maxInstructionsBefore)

    /**
     * Floating point literal.
     */
    constructor(
        literal : Double,
        opcodes: List<Opcode>? = null,
        maxInstructionsBefore: Int = METHOD_MAX_INSTRUCTIONS,
    ) : this({ literal.toRawBits() }, opcodes, maxInstructionsBefore)

    override fun matches(
        context: BytecodePatchContext,
        method: Method,
        instruction: Instruction,
        methodIndex: Int
    ): Boolean {
        if (!super.matches(context, method, instruction, methodIndex)) {
            return false
        }

        return (instruction as? WideLiteralInstruction)?.wideLiteral == literal()
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
class MethodCallFilter (
    /**
     * Defining class of the method call. Matches using endsWith().
     *
     * For calls to a method in the same class, use 'this' as the defining class.
     * Note: 'this' does not work for methods declared only in a superclass.
     */
    val definingClass: ((BytecodePatchContext) -> String)? = null,
    /**
     * Method name. Must be exact match of the method name.
     */
    val methodName: ((BytecodePatchContext) -> String)? = null,
    /**
     * Parameters of the method call. Each parameter matches
     * using startsWith() and semantics are the same as [Fingerprint].
     */
    val parameters: ((BytecodePatchContext) -> List<String>)? = null,
    /**
     * Return type. Matches using startsWith()
     */
    val returnType: ((BytecodePatchContext) -> String)? = null,
    /**
     * Opcode types to match. By default this matches any method call opcode:
     * <code>Opcode.INVOKE_*</code>.
     *
     * If this filter must match specific types of method call, then specify the desired opcodes
     * such as [Opcode.INVOKE_STATIC], [Opcode.INVOKE_STATIC_RANGE] to only match static calls.
     */
    opcodes: List<Opcode>? = null,
    maxInstructionsBefore: Int = METHOD_MAX_INSTRUCTIONS,
) : OpcodesFilter(opcodes, maxInstructionsBefore) {

    // Define both providers and literal strings.
    // Providers are used when the parameters are not known at declaration,
    // such as using another Fingerprint to find a class def or method name.
    @Suppress("USELESS_CAST")
    constructor(
        /**
         * Defining class of the method call. Matches using endsWith().
         *
         * For calls to a method in the same class, use 'this' as the defining class.
         * Note: 'this' does not work for methods declared only in a superclass.
         */
        definingClass: String? = null,
        /**
         * Method name. Must be exact match of the method name.
         */
        methodName: String? = null,
        /**
         * Parameters of the method call. Each parameter matches
         * using startsWith() and semantics are the same as [Fingerprint].
         */
        parameters: List<String>? = null,
        /**
         * Return type.  Matches using startsWith()
         */
        returnType: String? = null,
        /**
         * Opcode types to match. By default this matches any method call opcode:
         * <code>Opcode.INVOKE_*</code>.
         *
         * If this filter must match specific types of method call, then specify the desired opcodes
         * such as [Opcode.INVOKE_STATIC], [Opcode.INVOKE_STATIC_RANGE] to only match static calls.
         */
        opcodes: List<Opcode>? = null,
        maxInstructionsBefore: Int = METHOD_MAX_INSTRUCTIONS,
    ) : this(
        if (definingClass != null) {
            { context: BytecodePatchContext -> definingClass } as ((BytecodePatchContext) -> String)
        } else null, if (methodName != null) {
            { methodName }
        } else null, if (parameters != null) {
            { parameters }
        } else null, if (returnType != null) {
            { returnType }
        } else null,
        opcodes,
        maxInstructionsBefore
    )

    constructor(
        /**
         * Defining class of the method call. Matches using endsWith().
         *
         * For calls to a method in the same class, use 'this' as the defining class.
         * Note: 'this' does not work for methods declared only in a superclass.
         */
        definingClass: String? = null,
        /**
         * Method name. Must be exact match of the method name.
         */
        methodName: String? = null,
        /**
         * Parameters of the method call. Each parameter matches
         * using startsWith() and semantics are the same as [Fingerprint].
         */
        parameters: List<String>? = null,
        /**
         * Return type.  Matches using startsWith()
         */
        returnType: String? = null,
        /**
         * Single opcode this filter must match to.
         */
        opcode: Opcode,
        maxInstructionsBefore: Int = METHOD_MAX_INSTRUCTIONS,
    ) : this(definingClass, methodName, parameters , returnType, listOf(opcode), maxInstructionsBefore)

    override fun matches(
        context: BytecodePatchContext,
        method: Method,
        instruction: Instruction,
        methodIndex: Int
    ): Boolean {
        if (!super.matches(context, method, instruction, methodIndex)) {
            return false
        }

        val reference = (instruction as? ReferenceInstruction)?.reference as? MethodReference
        if (reference == null) return false

        if (definingClass != null) {
            val referenceClass = reference.definingClass
            val definingClass = definingClass(context)
            if (!referenceClass.endsWith(definingClass)) {
                // Check if 'this' defining class is used.
                // Would be nice if this also checked all super classes,
                // but doing so requires iteratively checking all superclasses
                // up to the root Object class since class defs are mere Strings.
                if (!(definingClass == "this" && referenceClass == method.definingClass)) {
                    return false
                } // else, the method call is for 'this' class.
            }
        }
        if (methodName != null && reference.name != methodName(context)) {
            return false
        }
        if (returnType != null && !reference.returnType.startsWith(returnType(context))) {
            return false
        }
        if (parameters != null && !parametersStartsWith(reference.parameterTypes, parameters(context))) {
            return false
        }

        return true
    }

    companion object {
        private val regex = Regex("""^(L[^;]+;)->([^(\s]+)\(([^)]*)\)(.+)$""")

        /**
         * Returns a filter for a copy pasted JVM-style method signature. e.g.:
         * Landroid/view/View;->inflate(Landroid/content/Context;ILandroid/view/ViewGroup;)Landroid/view/View;
         *
         * Does not support obfuscated method names or parameter/return types.
         */
        fun parseJvmMethodCall(
            methodSignature: String,
        ) = parseJvmMethodCall(methodSignature, null, METHOD_MAX_INSTRUCTIONS)

        /**
         * Returns a filter for a copy pasted JVM-style method signature. e.g.:
         * Landroid/view/View;->inflate(Landroid/content/Context;ILandroid/view/ViewGroup;)Landroid/view/View;
         *
         * Does not support obfuscated method names or parameter/return types.
         */
        fun parseJvmMethodCall(
            methodSignature: String,
            maxInstructionsBefore: Int
        ) = parseJvmMethodCall(methodSignature, null, maxInstructionsBefore)

        /**
         * Returns a filter for a copy pasted JVM-style method signature. e.g.:
         * Landroid/view/View;->inflate(Landroid/content/Context;ILandroid/view/ViewGroup;)Landroid/view/View;
         *
         * Does not support obfuscated method names or parameter/return types.
         */
        fun parseJvmMethodCall(
            methodSignature: String,
            opcodes: List<Opcode>?,
        ) = parseJvmMethodCall(methodSignature, opcodes, METHOD_MAX_INSTRUCTIONS)

        /**
         * Returns a filter for a copy pasted JVM-style method signature. e.g.:
         * Landroid/view/View;->inflate(Landroid/content/Context;ILandroid/view/ViewGroup;)Landroid/view/View;
         *
         * Does not support obfuscated method names or parameter/return types.
         */
        fun parseJvmMethodCall(
            methodSignature: String,
            opcodes: List<Opcode>?,
            maxInstructionsBefore: Int
        ): MethodCallFilter {
            val matchResult = regex.matchEntire(methodSignature)
                ?: throw IllegalArgumentException("Invalid method signature: $methodSignature")

            val classDescriptor = matchResult.groupValues[1]
            val methodName = matchResult.groupValues[2]
            val paramDescriptorString = matchResult.groupValues[3]
            val returnDescriptor = matchResult.groupValues[4]

            val paramDescriptors = parseParameterDescriptors(paramDescriptorString)

            return MethodCallFilter(
                classDescriptor,
                methodName,
                paramDescriptors,
                returnDescriptor,
                opcodes,
                maxInstructionsBefore
            )
        }

        /**
         * Parses a single JVM type descriptor or an array descriptor at the current position.
         * For example: Lcom/example/SomeClass; or I or [I or [Lcom/example/SomeClass; etc.
         */
        private fun parseSingleType(params: String, startIndex: Int): Pair<String, Int> {
            var i = startIndex

            // Keep track of array dimensions '['
            while (i < params.length && params[i] == '[') {
                i++
            }

            return if (i < params.length && params[i] == 'L') {
                // It's an object type starting with 'L', read until ';'
                val semicolonPos = params.indexOf(';', i)
                if (semicolonPos == -1) {
                    throw IllegalArgumentException("Malformed object descriptor (missing semicolon) in: $params")
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
         * Parses the parameters (the part inside parentheses) into a list of JVM type descriptors.
         */
        private fun parseParameterDescriptors(paramString: String): List<String> {
            val result = mutableListOf<String>()
            var currentIndex = 0
            while (currentIndex < paramString.length) {
                val (type, nextIndex) = parseSingleType(paramString, currentIndex)
                result.add(type)
                currentIndex = nextIndex
            }
            return result
        }
    }
}

/**
 * Matches a field call, such as:
 * `iget-object v0, p0, Lahhh;->g:Landroid/view/View;`
 */
class FieldCallFilter(
    /**
     * Defining class of the field call. Matches using endsWith().
     *
     * For calls to a method in the same class, use 'this' as the defining class.
     * Note: 'this' does not work for fields found in superclasses.
     */
    val definingClass: ((BytecodePatchContext) -> String)? = null,
    /**
     * Name of the field. Must be a full match of the field name.
     */
    val name: ((BytecodePatchContext) -> String)? = null,
    /**
     * Class type of field. Partial matches using startsWith() is allowed.
     */
    val type: ((BytecodePatchContext) -> String)? = null,
    opcodes: List<Opcode>? = null,
    maxInstructionsBefore: Int = METHOD_MAX_INSTRUCTIONS,
) : OpcodesFilter(opcodes, maxInstructionsBefore) {

    @Suppress("USELESS_CAST")
    constructor(
        /**
         * Defining class of the field call. Matches using endsWith().
         *
         * For calls to a method in the same class, use 'this' as the defining class.
         * Note: 'this' does not work for fields found in superclasses.
         */
        definingClass: String? = null,
        /**
         * Name of the field.  Must be a full match of the field name.
         */
        name: String? = null,
        /**
         * Class type of field. Partial matches using startsWith() is allowed.
         */
        type: String? = null,
        opcodes: List<Opcode>? = null,
        maxInstructionsBefore: Int = METHOD_MAX_INSTRUCTIONS,
    ) : this(
        if (definingClass != null) {
            { context: BytecodePatchContext -> definingClass } as ((BytecodePatchContext) -> String)
        } else null,
        if (name != null) {
            { name }
        } else null,
        if (type != null) {
            { type }
        } else null,
        opcodes,
        maxInstructionsBefore
    )

    constructor(
        /**
         * Defining class of the field call. Matches using endsWith().
         *
         * For calls to a method in the same class, use 'this' as the defining class.
         * Note: 'this' does not work for fields found in superclasses.
         */
        definingClass: String? = null,
        /**
         * Name of the field.  Must be a full match of the field name.
         */
        name: String? = null,
        /**
         * Class type of field. Partial matches using startsWith() is allowed.
         */
        type: String? = null,
        opcode: Opcode,
        maxInstructionsBefore: Int = METHOD_MAX_INSTRUCTIONS,
    ) : this(definingClass, name, type, listOf(opcode), maxInstructionsBefore)

    override fun matches(
        context: BytecodePatchContext,
        method: Method,
        instruction: Instruction,
        methodIndex: Int
    ): Boolean {
        if (!super.matches(context, method, instruction, methodIndex)) {
            return false
        }

        val reference = (instruction as? ReferenceInstruction)?.reference as? FieldReference
        if (reference == null) return false

        if (definingClass != null) {
            val referenceClass = reference.definingClass
            val definingClass = definingClass(context)

            if (!referenceClass.endsWith(definingClass)) {
                if (!(definingClass == "this" && referenceClass == method.definingClass)) {
                    return false
                } // else, the method call is for 'this' class.
            }
        }
        if (name != null && reference.name != name(context)) {
            return false
        }
        if (type != null && !reference.type.startsWith(type(context))) {
            return false
        }

        return true
    }
}

/**
 * Opcode type NEW_INSTANCE or NEW_ARRAY, with a non obfuscated class type.
 */
class NewInstanceFilter(
    val type: String,
    maxInstructionsBefore: Int = METHOD_MAX_INSTRUCTIONS,
) : OpcodesFilter(listOf(Opcode.NEW_INSTANCE, Opcode.NEW_ARRAY), maxInstructionsBefore) {

    override fun matches(
        context: BytecodePatchContext,
        method: Method,
        instruction: Instruction,
        methodIndex: Int
    ): Boolean {
        if (!super.matches(context, method, instruction, methodIndex)) {
            return false
        }

        val reference = (instruction as? ReferenceInstruction)?.reference as? TypeReference
        if (reference == null) return false

        return reference.type == type
    }
}

/**
 * Filter wrapper that only matches the last instruction of a method.
 */
class LastInstructionFilter(
    var filter : InstructionFilter,
    maxInstructionsBefore: Int = METHOD_MAX_INSTRUCTIONS,
) : InstructionFilter(maxInstructionsBefore) {

    override fun matches(
        context: BytecodePatchContext,
        method: Method,
        instruction: Instruction,
        methodIndex: Int
    ): Boolean {
        return methodIndex == method.instructions.count() - 1 && filter.matches(
            context, method, instruction, methodIndex
        )
    }
}
