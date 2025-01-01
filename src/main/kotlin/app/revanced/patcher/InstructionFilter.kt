package app.revanced.patcher

import app.revanced.patcher.InstructionFilter.Companion.METHOD_MAX_INSTRUCTIONS
import app.revanced.patcher.extensions.InstructionExtensions.instructions
import com.android.tools.smali.dexlib2.Opcode
import com.android.tools.smali.dexlib2.iface.ClassDef
import com.android.tools.smali.dexlib2.iface.Method
import com.android.tools.smali.dexlib2.iface.instruction.Instruction
import com.android.tools.smali.dexlib2.iface.instruction.ReferenceInstruction
import com.android.tools.smali.dexlib2.iface.instruction.WideLiteralInstruction
import com.android.tools.smali.dexlib2.iface.reference.FieldReference
import com.android.tools.smali.dexlib2.iface.reference.MethodReference
import java.util.EnumSet
import kotlin.collections.forEach

interface InstructionFilter {
    /**
     * Maximum number of non matching instructions that can be before this filter.
     * A value of zero means this filter must match immediately after the prior filter,
     * or if this is the first filter then this may only match the first instruction of the method.
     */
    val maxInstructionsBefore: Int

    fun matches(
        classDef: ClassDef,
        method: Method,
        instruction: Instruction,
        methodIndex: Int
    ): Boolean

    companion object {
        /**
         * Maximum number of instructions allowed in a Java method.
         */
        const val METHOD_MAX_INSTRUCTIONS = 65535
    }
}

/**
 * Logical or operator, where the first filter that matches is the match result.
 */
class AnyFilter(
    private val filters: List<InstructionFilter>,
    override val maxInstructionsBefore: Int = METHOD_MAX_INSTRUCTIONS,
) : InstructionFilter {
    override fun matches(
        classDef: ClassDef,
        method: Method,
        instruction: Instruction,
        methodIndex: Int
    ): Boolean {
        return filters.any { matches(classDef, method, instruction, methodIndex) }
    }
}

/**
 * Allows matching a method using the result of a previously resolved [Fingerprint].
 * Useful for complex matching.
 */
class MethodFingerprintFilter(
    private val fingerprint: Fingerprint,
    override val maxInstructionsBefore: Int = METHOD_MAX_INSTRUCTIONS,
) : InstructionFilter {
    override fun matches(
        classDef: ClassDef,
        method: Method,
        instruction: Instruction,
        methodIndex: Int
    ): Boolean {
        val match = fingerprint._matchOrNull!!
        return classDef == match.originalClassDef &&
                method == match.originalMethod
    }
}



/**
 * Single opcode.
 */
class OpcodeFilter(
    val opcode: Opcode,
    override val maxInstructionsBefore: Int = METHOD_MAX_INSTRUCTIONS,
) : InstructionFilter {

    override fun matches(
        classDef: ClassDef,
        method: Method,
        instruction: Instruction,
        methodIndex: Int
    ): Boolean {
        return instruction.opcode == opcode
    }

    companion object {
        fun listOfOpcodes(opcodes: Collection<Opcode?>): List<OpcodeFilter> {
            var list = ArrayList<OpcodeFilter>(opcodes.size)

            // First opcode can match anywhere.
            var instructionsBefore = METHOD_MAX_INSTRUCTIONS
            opcodes.forEach { opcode ->
                if (opcode == null) {
                    // Allow a non match or a missing instruction.
                    instructionsBefore++
                } else {
                    list += OpcodeFilter(opcode, instructionsBefore)
                    instructionsBefore = 0
                }
            }

            return list
        }
    }
}

/**
 * Matches multiple opcodes.
 * If using only a single opcode instead use [OpcodeFilter].
 */
open class OpcodesFilter(
    val opcodes: EnumSet<Opcode>?,
    override val maxInstructionsBefore: Int = METHOD_MAX_INSTRUCTIONS,
) : InstructionFilter {

    constructor(
        opcodes: List<Opcode>?,
        maxInstructionsBefore: Int = METHOD_MAX_INSTRUCTIONS
    ) : this(if (opcodes == null) null else EnumSet.copyOf(opcodes))

    override fun matches(
        classDef: ClassDef,
        method: Method,
        instruction: Instruction,
        methodIndex: Int
    ): Boolean {
        if (opcodes == null) {
            return true // Match anything.
        }
        return opcodes.contains(instruction.opcode) == true
    }
}

class LiteralFilter(
    var literal: Long,
    override val maxInstructionsBefore: Int = METHOD_MAX_INSTRUCTIONS,
    opcodes: List<Opcode>? = null,
) : OpcodesFilter(opcodes, maxInstructionsBefore) {

    /**
     * Floating point literal.
     */
    constructor(
        literal: Double,
        maxInstructionsBefore: Int = METHOD_MAX_INSTRUCTIONS,
        opcodes: List<Opcode>? = null,
    ) : this(literal.toRawBits(), maxInstructionsBefore, opcodes)

    override fun matches(
        classDef: ClassDef,
        method: Method,
        instruction: Instruction,
        methodIndex: Int
    ): Boolean {
        if (!super.matches(classDef, method, instruction, methodIndex)) {
            return false
        }

        return (instruction as? WideLiteralInstruction)?.wideLiteral == literal
    }
}

class MethodFilter(
    /**
     * Defining class of the method call. Matches using endsWith().
     *
     * For calls to a method in the same class, use 'this'
     * as the defining class. Note: 'this' does not work for methods declared only in a superclass.
     */
    val definingClass: String? = null,
    /**
     * Method name. Must be exact match of the method name.
     */
    val methodName: String? = null,
    /**
     * Parameters of the method call. Each parameter matches
     * using startsWith() and semantics are the same as [Fingerprint].
     */
    val parameters: List<String>? = null,
    /**
     * Return type.  Matches using startsWith().;
     */
    val returnType: String? = null,
    /**
     * Opcode types to match. By default this matches any method call opcode:
     * <code>Opcode.INVOKE_*</code>.
     *
     * If this filter must match specific types of method call, then define which
     * such as [Opcode.INVOKE_STATIC], [Opcode.INVOKE_STATIC_RANGE] to only match a static call.
     */
    opcodes: List<Opcode>? = null,
    maxInstructionsBefore: Int = METHOD_MAX_INSTRUCTIONS,
) : OpcodesFilter(opcodes, maxInstructionsBefore) {

    override fun matches(
        classDef: ClassDef,
        method: Method,
        instruction: Instruction,
        methodIndex: Int
    ): Boolean {
        if (!super.matches(classDef, method, instruction, methodIndex)) {
            return false
        }

        val reference = (instruction as? ReferenceInstruction)?.reference as? MethodReference
        if (reference == null) return false

        if (definingClass != null) {
            val referenceClass = reference.definingClass
            if (!referenceClass.endsWith(definingClass)) {
                // Check if 'this' defining class is used.
                // Would be nice if this also checked all super classes,
                // but doing so requires iteratively checking all superclasses
                // up to the root Object class since class defs are mere Strings.
                if (definingClass != "this" || referenceClass != classDef.type) {
                    return false
                } // else, the method call is for 'this' class.
            }
        }
        if (methodName != null && reference.name != methodName) {
            return false
        }
        if (returnType != null && !reference.returnType.startsWith(returnType)) {
            return false
        }
        if (parameters != null && !parametersEqual(parameters, method.parameterTypes)) {
            return false
        }

        return true
    }
}

class FieldFilter(
    /**
     * Defining class of the field call. For calls to a method in the same class, use 'this'
     * as the defining class. Not: 'this' does not work for fields found in superclasses.
     * Matches using endsWith().
     */
    val definingClass: String? = null,
    /**
     * Name of the field.  Must be a full match of the field name.
     */
    val name: String? = null,
    /**
     * Class type of field. Partial matches using startsWith() is allowed.
     */
    val type: String? = null,
    opcodes: List<Opcode>? = null,
    override val maxInstructionsBefore: Int = METHOD_MAX_INSTRUCTIONS,
) : OpcodesFilter(opcodes, maxInstructionsBefore) {

    override fun matches(
        classDef: ClassDef,
        method: Method,
        instruction: Instruction,
        methodIndex: Int
    ): Boolean {
        if (!super.matches(classDef, method, instruction, methodIndex)) {
            return false
        }

        val reference = (instruction as? ReferenceInstruction)?.reference as? FieldReference
        if (reference == null) return false

        if (definingClass != null) {
            val referenceClass = reference.definingClass
            if (!referenceClass.endsWith(definingClass)) {
                if (definingClass != "this" || referenceClass != classDef.type) {
                    return false
                } // else, the method call is for 'this' class.
            }
        }
        if (name != null && reference.name !=name) {
            return false
        }
        if (type != null && !reference.type.startsWith(type)) {
            return false
        }

        return true
    }
}

/**
 * Filter wrapper that only matches the last instruction of a method.
 */
class LastInstructionFilter(
    var filter : InstructionFilter,
    override val maxInstructionsBefore: Int = METHOD_MAX_INSTRUCTIONS,
) : InstructionFilter {

    override fun matches(
        classDef: ClassDef,
        method: Method,
        instruction: Instruction,
        methodIndex: Int
    ): Boolean {
        return (methodIndex == method.instructions.count() - 1 && filter.matches(
            classDef, method, instruction, methodIndex
        ))
    }
}

