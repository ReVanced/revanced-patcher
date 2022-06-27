package app.revanced.patcher.extensions

import app.revanced.patcher.util.proxy.mutableTypes.MutableMethod
import app.revanced.patcher.util.proxy.mutableTypes.MutableMethod.Companion.toMutable
import app.revanced.patcher.util.smali.toInstruction
import app.revanced.patcher.util.smali.toInstructions
import org.jf.dexlib2.AccessFlags
import org.jf.dexlib2.builder.BuilderInstruction
import org.jf.dexlib2.builder.BuilderOffsetInstruction
import org.jf.dexlib2.builder.Label
import org.jf.dexlib2.builder.MutableMethodImplementation
import org.jf.dexlib2.builder.instruction.*
import org.jf.dexlib2.iface.Method
import org.jf.dexlib2.iface.reference.MethodReference
import org.jf.dexlib2.immutable.ImmutableMethod
import org.jf.dexlib2.immutable.ImmutableMethodImplementation
import org.jf.dexlib2.util.MethodUtil
import java.io.OutputStream

infix fun AccessFlags.or(other: AccessFlags) = this.value or other.value
infix fun Int.or(other: AccessFlags) = this or other.value

fun MutableMethodImplementation.addInstructions(index: Int, instructions: List<BuilderInstruction>) {
    for (i in instructions.lastIndex downTo 0) {
        this.addInstruction(index, instructions[i])
    }
}

fun MutableMethodImplementation.addInstructions(instructions: List<BuilderInstruction>) {
    for (instruction in instructions) {
        this.addInstruction(instruction)
    }
}

fun MutableMethodImplementation.replaceInstructions(index: Int, instructions: List<BuilderInstruction>) {
    for (i in instructions.lastIndex downTo 0) {
        this.replaceInstruction(index + i, instructions[i])
    }
}

fun MutableMethodImplementation.removeInstructions(index: Int, count: Int) {
    for (i in count downTo 0) {
        this.removeInstruction(index + i)
    }
}

/**
 * Compare a method to another, considering constructors and parameters.
 * @param otherMethod The method to compare against.
 * @return True if the methods match given the conditions.
 */
fun Method.softCompareTo(otherMethod: MethodReference): Boolean {
    if (MethodUtil.isConstructor(this) && !parametersEqual(this.parameterTypes, otherMethod.parameterTypes))
        return false
    return this.name == otherMethod.name
}

/**
 * Clones the method.
 * @param registerCount This parameter allows you to change the register count of the method.
 * This may be a positive or negative number.
 * @return The **immutable** cloned method. Call [toMutable] or [cloneMutable] to get a **mutable** copy.
 */
internal fun Method.clone(registerCount: Int = 0): ImmutableMethod {
    val clonedImplementation = implementation?.let {
        ImmutableMethodImplementation(
            it.registerCount + registerCount,
            it.instructions,
            it.tryBlocks,
            it.debugItems,
        )
    }
    return ImmutableMethod(
        returnType,
        name,
        parameters,
        returnType,
        accessFlags,
        annotations,
        hiddenApiRestrictions,
        clonedImplementation
    )
}

/**
 * Add a smali instruction to the method.
 * @param instruction The smali instruction to add.
 */
fun MutableMethod.addInstruction(instruction: String) =
    this.implementation!!.addInstruction(instruction.toInstruction(this))

/**
 * Add a smali instruction to the method.
 * @param index The index to insert the instruction at.
 * @param instruction The smali instruction to add.
 */
fun MutableMethod.addInstruction(index: Int, instruction: String) =
    this.implementation!!.addInstruction(index, instruction.toInstruction(this))

/**
 * Replace a smali instruction within the method.
 * @param index The index to replace the instruction at.
 * @param instruction The smali instruction to place.
 */
fun MutableMethod.replaceInstruction(index: Int, instruction: String) =
    this.implementation!!.replaceInstruction(index, instruction.toInstruction(this))

/**
 * Remove a smali instruction within the method.
 * @param index The index to delete the instruction at.
 */
fun MutableMethod.removeInstruction(index: Int) =
    this.implementation!!.removeInstruction(index)

/**
 * Add smali instructions to the method.
 * @param index The index to insert the instructions at.
 * @param smali The smali instructions to add.
 */
fun MutableMethod.addInstructions(index: Int, smali: String, labels: List<Pair<String, Label>> = emptyList()) {
    var code = smali
    for ((name, _) in labels) {
        code += "\n :$name \n nop"
    }
    val instructions = code.toInstructions(this).toMutableList()
    var fixedInstructions: List<Int>? = null // find a better way to do this.

    if (labels.isNotEmpty()) {
        val labelRange = instructions.size - labels.size..instructions.size
        fixedInstructions = mutableListOf()
        for (instructionIndex in 0 until instructions.size - labels.size) {
            val instruction = instructions[instructionIndex]
            if (instruction !is BuilderOffsetInstruction || !instruction.target.isPlaced) continue
            val fakeIndex = instruction.target.location.index
            if (!labelRange.contains(fakeIndex)) continue
            instructions[instructionIndex] = replaceOffset(instruction, labels[labelRange.indexOf(fakeIndex)].second)
            fixedInstructions.add(instructionIndex + index)
        }
        // find a better way to drop the nop instructions.
        instructions.subList(labelRange.first, labelRange.last).clear()
    }

    this.implementation!!.addInstructions(index, instructions)
    this.fixInstructions(index, instructions, fixedInstructions)
}

/**
 * Add smali instructions to the end of the method.
 * @param instructions The smali instructions to add.
 */
fun MutableMethod.addInstructions(instructions: String, labels: List<Pair<String, Label>> = emptyList()) =
    this.addInstructions(this.implementation!!.instructions.size, instructions, labels)

/**
 * Replace smali instructions within the method.
 * @param index The index to replace the instructions at.
 * @param instructions The smali instructions to place.
 */
fun MutableMethod.replaceInstructions(index: Int, instructions: String) =
    this.implementation!!.replaceInstructions(index, instructions.toInstructions(this))

/**
 * Remove smali instructions from the method.
 * @param index The index to remove the instructions at.
 * @param count The amount of instructions to remove.
 */
fun MutableMethod.removeInstructions(index: Int, count: Int) =
    this.implementation!!.removeInstructions(index, count)

fun MutableMethod.label(index: Int) = this.implementation!!.newLabelForIndex(index)
fun MutableMethod.instruction(index: Int): BuilderInstruction = this.implementation!!.instructions[index]

private fun MutableMethod.fixInstructions(index: Int, instructions: List<BuilderInstruction>, skipInstructions: List<Int>?) {
    for (instructionIndex in index until instructions.size + index) {
        val instruction = this.implementation!!.instructions[instructionIndex]
        if (instruction !is BuilderOffsetInstruction || !instruction.target.isPlaced) continue
        if (skipInstructions?.contains(instructionIndex) == true) continue
        val fakeIndex = instruction.target.location.index
        val fixedIndex = fakeIndex + index
        if (fakeIndex == fixedIndex) continue // no need to replace if the indexes are the same.
        this.implementation!!.replaceInstruction(instructionIndex, replaceOffset(instruction, this.label(fixedIndex)))
    }
}

private fun replaceOffset(
    i: BuilderOffsetInstruction,
    label: Label
): BuilderOffsetInstruction {
    return when (i) {
        is BuilderInstruction10t -> BuilderInstruction10t(i.opcode, label)
        is BuilderInstruction20t -> BuilderInstruction20t(i.opcode, label)
        is BuilderInstruction21t -> BuilderInstruction21t(i.opcode, i.registerA, label)
        is BuilderInstruction22t -> BuilderInstruction22t(i.opcode, i.registerA, i.registerB, label)
        is BuilderInstruction30t -> BuilderInstruction30t(i.opcode, label)
        is BuilderInstruction31t -> BuilderInstruction31t(i.opcode, i.registerA, label)
        else -> throw IllegalStateException("A non-offset instruction was given, this should never happen!")
    }
}

/**
 * Clones the method.
 * @param registerCount This parameter allows you to change the register count of the method.
 * This may be a positive or negative number.
 * @return The **mutable** cloned method. Call [clone] to get an **immutable** copy.
 */
internal fun Method.cloneMutable(registerCount: Int = 0) = clone(registerCount).toMutable()

// FIXME: also check the order of parameters as different order equals different method overload
internal fun parametersEqual(
    parameters1: Iterable<CharSequence>,
    parameters2: Iterable<CharSequence>
): Boolean {
    return parameters1.count() == parameters2.count() && parameters1.all { parameter ->
        parameters2.any {
            it.startsWith(
                parameter
            )
        }
    }
}

internal val nullOutputStream = object : OutputStream() {
    override fun write(b: Int) {}
}

/**
 * Should be used to parse a list of parameters represented by their first letter,
 * or in the case of arrays prefixed with an unspecified amount of '[' character.
 */
internal fun String.parseParameters(): List<String> {
    val parameters = mutableListOf<String>()
    var parameter = ""
    for (char in this.toCharArray()) {
        parameter += char
        if (char == '[') continue

        parameters.add(parameter)
        parameter = ""
    }
    return parameters
}