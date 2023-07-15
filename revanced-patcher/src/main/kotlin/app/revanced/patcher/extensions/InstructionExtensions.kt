package app.revanced.patcher.extensions

import app.revanced.patcher.util.proxy.mutableTypes.MutableMethod
import app.revanced.patcher.util.smali.ExternalLabel
import app.revanced.patcher.util.smali.toInstruction
import app.revanced.patcher.util.smali.toInstructions
import org.jf.dexlib2.builder.BuilderInstruction
import org.jf.dexlib2.builder.BuilderOffsetInstruction
import org.jf.dexlib2.builder.Label
import org.jf.dexlib2.builder.MutableMethodImplementation
import org.jf.dexlib2.builder.instruction.*
import org.jf.dexlib2.iface.instruction.Instruction

object InstructionExtensions {

    /**
     * Add instructions to a method at the given index.
     *
     * @param index The index to add the instructions at.
     * @param instructions The instructions to add.
     */
    fun MutableMethodImplementation.addInstructions(
        index: Int,
        instructions: List<BuilderInstruction>
    ) =
        instructions.asReversed().forEach { addInstruction(index, it) }

    /**
     * Add instructions to a method.
     * The instructions will be added at the end of the method.
     *
     * @param instructions The instructions to add.
     */
    fun MutableMethodImplementation.addInstructions(instructions: List<BuilderInstruction>) =
        instructions.forEach { this.addInstruction(it) }

    /**
     * Remove instructions from a method at the given index.
     *
     * @param index The index to remove the instructions at.
     * @param count The amount of instructions to remove.
     */
    fun MutableMethodImplementation.removeInstructions(index: Int, count: Int) = repeat(count) {
        removeInstruction(index)
    }

    /**
     * Remove the first instructions from a method.
     *
     * @param count The amount of instructions to remove.
     */
    fun MutableMethodImplementation.removeInstructions(count: Int) = removeInstructions(0, count)

    /**
     * Replace instructions at the given index with the given instructions.
     * The amount of instructions to replace is the amount of instructions in the given list.
     *
     * @param index The index to replace the instructions at.
     * @param instructions The instructions to replace the instructions with.
     */
    fun MutableMethodImplementation.replaceInstructions(index: Int, instructions: List<BuilderInstruction>) {
        // Remove the instructions at the given index.
        removeInstructions(index, instructions.size)

        // Add the instructions at the given index.
        addInstructions(index, instructions)
    }

    /**
     * Add an instruction to a method at the given index.
     *
     * @param index The index to add the instruction at.
     * @param instruction The instruction to add.
     */
    fun MutableMethod.addInstruction(index: Int, instruction: BuilderInstruction) =
        implementation!!.addInstruction(index, instruction)

    /**
     * Add an instruction to a method.
     *
     * @param instruction The instructions to add.
     */
    fun MutableMethod.addInstruction(instruction: BuilderInstruction) =
        implementation!!.addInstruction(instruction)

    /**
     * Add an instruction to a method at the given index.
     *
     * @param index The index to add the instruction at.
     * @param smaliInstructions The instruction to add.
     */
    fun MutableMethod.addInstruction(index: Int, smaliInstructions: String) =
        implementation!!.addInstruction(index, smaliInstructions.toInstruction(this))

    /**
     * Add an instruction to a method.
     *
     * @param smaliInstructions The instruction to add.
     */
    fun MutableMethod.addInstruction(smaliInstructions: String) =
        implementation!!.addInstruction(smaliInstructions.toInstruction(this))


    /**
     * Add instructions to a method at the given index.
     *
     * @param index The index to add the instructions at.
     * @param instructions The instructions to add.
     */
    fun MutableMethod.addInstructions(index: Int, instructions: List<BuilderInstruction>) =
        implementation!!.addInstructions(index, instructions)

    /**
     * Add instructions to a method.
     *
     * @param instructions The instructions to add.
     */
    fun MutableMethod.addInstructions(instructions: List<BuilderInstruction>) =
        implementation!!.addInstructions(instructions)

    /**
     * Add instructions to a method.
     *
     * @param smaliInstructions The instructions to add.
     */
    fun MutableMethod.addInstructions(index: Int, smaliInstructions: String) =
        implementation!!.addInstructions(index, smaliInstructions.toInstructions(this))

    /**
     * Add instructions to a method.
     *
     * @param smaliInstructions The instructions to add.
     */
    fun MutableMethod.addInstructions(smaliInstructions: String) =
        implementation!!.addInstructions(smaliInstructions.toInstructions(this))

    /**
     * Add instructions to a method at the given index.
     *
     * @param index The index to add the instructions at.
     * @param smaliInstructions The instructions to add.
     * @param externalLabels A list of [ExternalLabel] for instructions outside of [smaliInstructions].
     */
// Special function for adding instructions with external labels.
    fun MutableMethod.addInstructionsWithLabels(
        index: Int,
        smaliInstructions: String,
        vararg externalLabels: ExternalLabel
    ) {
        // Create reference dummy instructions for the instructions.
        val nopSmali = StringBuilder(smaliInstructions).also { builder ->
            externalLabels.forEach { (name, _) ->
                builder.append("\n:$name\nnop")
            }
        }.toString()

        // Compile the instructions with the dummy labels
        val compiledInstructions = nopSmali.toInstructions(this)

        // Add the compiled list of instructions to the method.
        addInstructions(
            index,
            compiledInstructions.subList(0, compiledInstructions.size - externalLabels.size)
        )

        implementation!!.apply {
            this@apply.instructions.subList(index, index + compiledInstructions.size - externalLabels.size)
                .forEachIndexed { compiledInstructionIndex, compiledInstruction ->
                    // If the compiled instruction is not an offset instruction, skip it.
                    if (compiledInstruction !is BuilderOffsetInstruction) return@forEachIndexed

                    /**
                     * Creates a new label for the instruction
                     * and replaces it with the label of the [compiledInstruction] at [compiledInstructionIndex].
                     */
                    fun Instruction.makeNewLabel() {
                        fun replaceOffset(
                            i: BuilderOffsetInstruction, label: Label
                        ): BuilderOffsetInstruction {
                            return when (i) {
                                is BuilderInstruction10t -> BuilderInstruction10t(i.opcode, label)
                                is BuilderInstruction20t -> BuilderInstruction20t(i.opcode, label)
                                is BuilderInstruction21t -> BuilderInstruction21t(i.opcode, i.registerA, label)
                                is BuilderInstruction22t -> BuilderInstruction22t(
                                    i.opcode,
                                    i.registerA,
                                    i.registerB,
                                    label
                                )
                                is BuilderInstruction30t -> BuilderInstruction30t(i.opcode, label)
                                is BuilderInstruction31t -> BuilderInstruction31t(i.opcode, i.registerA, label)
                                else -> throw IllegalStateException(
                                    "A non-offset instruction was given, this should never happen!"
                                )
                            }
                        }

                        // Create the final label.
                        val label = newLabelForIndex(this@apply.instructions.indexOf(this))

                        // Create the final instruction with the new label.
                        val newInstruction = replaceOffset(
                            compiledInstruction, label
                        )

                        // Replace the instruction pointing to the dummy label
                        // with the new instruction pointing to the real instruction.
                        replaceInstruction(index + compiledInstructionIndex, newInstruction)
                    }

                    // If the compiled instruction targets its own instruction,
                    // which means it points to some of its own, simply an offset has to be applied.
                    val labelIndex = compiledInstruction.target.location.index
                    if (labelIndex < compiledInstructions.size - externalLabels.size) {
                        // Get the targets index (insertion index + the index of the dummy instruction).
                        this.instructions[index + labelIndex].makeNewLabel()
                        return@forEachIndexed
                    }

                    // Since the compiled instruction points to a dummy instruction,
                    // we can find the real instruction which it was created for by calculation.

                    // Get the index of the instruction in the externalLabels list
                    // which the dummy instruction was created for.
                    // This works because we created the dummy instructions in the same order as the externalLabels list.
                    val (_, instruction) = externalLabels[(compiledInstructions.size - 1) - labelIndex]
                    instruction.makeNewLabel()
                }
        }
    }

    /**
     * Remove an instruction at the given index.
     *
     * @param index The index to remove the instruction at.
     */
    fun MutableMethod.removeInstruction(index: Int) =
        implementation!!.removeInstruction(index)

    /**
     * Remove instructions at the given index.
     *
     * @param index The index to remove the instructions at.
     * @param count The amount of instructions to remove.
     */
    fun MutableMethod.removeInstructions(index: Int, count: Int) =
        implementation!!.removeInstructions(index, count)

    /**
     * Remove instructions at the given index.
     *
     * @param count The amount of instructions to remove.
     */
    fun MutableMethod.removeInstructions(count: Int) =
        implementation!!.removeInstructions(count)

    /**
     * Replace an instruction at the given index.
     *
     * @param index The index to replace the instruction at.
     * @param instruction The instruction to replace the instruction with.
     */
    fun MutableMethod.replaceInstruction(index: Int, instruction: BuilderInstruction) =
        implementation!!.replaceInstruction(index, instruction)

    /**
     * Replace an instruction at the given index.
     *
     * @param index The index to replace the instruction at.
     * @param smaliInstruction The smali instruction to replace the instruction with.
     */
    fun MutableMethod.replaceInstruction(index: Int, smaliInstruction: String) =
        implementation!!.replaceInstruction(index, smaliInstruction.toInstruction(this))

    /**
     * Replace instructions at the given index.
     *
     * @param index The index to replace the instructions at.
     * @param instructions The instructions to replace the instructions with.
     */
    fun MutableMethod.replaceInstructions(index: Int, instructions: List<BuilderInstruction>) =
        implementation!!.replaceInstructions(index, instructions)

    /**
     * Replace instructions at the given index.
     *
     * @param index The index to replace the instructions at.
     * @param smaliInstructions The smali instructions to replace the instructions with.
     */
    fun MutableMethod.replaceInstructions(index: Int, smaliInstructions: String) =
        implementation!!.replaceInstructions(index, smaliInstructions.toInstructions(this))

    /**
     * Get an instruction at the given index.
     *
     * @param index The index to get the instruction at.
     * @return The instruction.
     */
    fun MutableMethodImplementation.getInstruction(index: Int): BuilderInstruction = instructions[index]

    /**
     * Get an instruction at the given index.
     *
     * @param index The index to get the instruction at.
     * @param T The type of instruction to return.
     * @return The instruction.
     */
    @Suppress("UNCHECKED_CAST")
    fun <T> MutableMethodImplementation.getInstruction(index: Int): T = getInstruction(index) as T

    /**
     * Get an instruction at the given index.
     * @param index The index to get the instruction at.
     * @return The instruction.
     */
    fun MutableMethod.getInstruction(index: Int): BuilderInstruction = implementation!!.getInstruction(index)

    /**
     * Get an instruction at the given index.
     * @param index The index to get the instruction at.
     * @param T The type of instruction to return.
     * @return The instruction.
     */
    @Suppress("UNCHECKED_CAST")
    fun <T> MutableMethod.getInstruction(index: Int): T = implementation!!.getInstruction<T>(index)
}