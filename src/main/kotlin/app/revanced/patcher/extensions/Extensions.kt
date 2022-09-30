package app.revanced.patcher.extensions

import app.revanced.patcher.util.proxy.mutableTypes.MutableMethod
import app.revanced.patcher.util.proxy.mutableTypes.MutableMethod.Companion.toMutable
import app.revanced.patcher.util.smali.ExternalLabel
import app.revanced.patcher.util.smali.toInstruction
import app.revanced.patcher.util.smali.toInstructions
import org.jf.dexlib2.AccessFlags
import org.jf.dexlib2.builder.BuilderInstruction
import org.jf.dexlib2.builder.BuilderOffsetInstruction
import org.jf.dexlib2.builder.Label
import org.jf.dexlib2.builder.MutableMethodImplementation
import org.jf.dexlib2.builder.instruction.*
import org.jf.dexlib2.iface.Method
import org.jf.dexlib2.iface.instruction.Instruction
import org.jf.dexlib2.immutable.ImmutableMethod
import org.jf.dexlib2.immutable.ImmutableMethodImplementation
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
        returnType, name, parameters, returnType, accessFlags, annotations, hiddenApiRestrictions, clonedImplementation
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
fun MutableMethod.removeInstruction(index: Int) = this.implementation!!.removeInstruction(index)

/**
 * Create a label for the instruction at given index in the method's implementation.
 * @param index The index to create the label for the instruction at.
 * @return The label.
 */
fun MutableMethod.label(index: Int) = this.implementation!!.newLabelForIndex(index)

/**
 * Get the instruction at the given index in the method's implementation.
 * @param index The index to get the instruction at.
 * @return The instruction.
 */
fun MutableMethod.instruction(index: Int): BuilderInstruction = this.implementation!!.instructions[index]

/**
 * Add smali instructions to the method.
 * @param index The index to insert the instructions at.
 * @param smali The smali instructions to add.
 * @param externalLabels A list of [ExternalLabel] representing a list of labels for instructions which are not in the method to compile.
 */

fun MutableMethod.addInstructions(index: Int, smali: String, externalLabels: List<ExternalLabel> = emptyList()) {
    // Create reference dummy instructions for the instructions.
    val nopedSmali = StringBuilder(smali).also { builder ->
        externalLabels.forEach { (name, _) ->
            builder.append("\n:$name\nnop")
        }
    }.toString()

    // Compile the instructions with the dummy labels
    val compiledInstructions = nopedSmali.toInstructions(this)

    // Add the compiled list of instructions to the method.
    val methodImplementation = this.implementation!!
    methodImplementation.addInstructions(index, compiledInstructions.subList(0, compiledInstructions.size - externalLabels.size))

    val methodInstructions = methodImplementation.instructions
    methodInstructions.subList(index, index + compiledInstructions.size - externalLabels.size)
        .forEachIndexed { compiledInstructionIndex, compiledInstruction ->
            // If the compiled instruction is not an offset instruction, skip it.
            if (compiledInstruction !is BuilderOffsetInstruction) return@forEachIndexed

            /**
             * Creates a new label for the instruction and replaces it with the label of the [compiledInstruction] at [compiledInstructionIndex].
             */
            fun Instruction.makeNewLabel() {
                // Create the final label.
                val label = methodImplementation.newLabelForIndex(methodInstructions.indexOf(this))
                // Create the final instruction with the new label.
                val newInstruction = replaceOffset(
                    compiledInstruction, label
                )
                // Replace the instruction pointing to the dummy label with the new instruction pointing to the real instruction.
                methodImplementation.replaceInstruction(index + compiledInstructionIndex, newInstruction)
            }

            // If the compiled instruction targets its own instruction,
            // which means it points to some of its own, simply an offset has to be applied.
            val labelIndex = compiledInstruction.target.location.index
            if (labelIndex < compiledInstructions.size - externalLabels.size) {
                // Get the targets index (insertion index + the index of the dummy instruction).
                methodInstructions[index + labelIndex].makeNewLabel()
                return@forEachIndexed
            }

            // Since the compiled instruction points to a dummy instruction,
            // we can find the real instruction which it was created for by calculation.

            // Get the index of the instruction in the externalLabels list which the dummy instruction was created for.
            // this line works because we created the dummy instructions in the same order as the externalLabels list.
            val (_, instruction) = externalLabels[(compiledInstructions.size - 1) - labelIndex]
            instruction.makeNewLabel()
        }
}

/**
 * Add smali instructions to the end of the method.
 * @param instructions The smali instructions to add.
 */
fun MutableMethod.addInstructions(instructions: String, labels: List<ExternalLabel> = emptyList()) =
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
fun MutableMethod.removeInstructions(index: Int, count: Int) = this.implementation!!.removeInstructions(index, count)

private fun replaceOffset(
    i: BuilderOffsetInstruction, label: Label
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

// FIXME: Also check the order of parameters as different order equals different method overload.
internal fun parametersEqual(
    parameters1: Iterable<CharSequence>, parameters2: Iterable<CharSequence>
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