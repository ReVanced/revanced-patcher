package app.revanced.patcher.util.smali

import org.jf.dexlib2.iface.instruction.Instruction

/**
 * A class that represents a label for an instruction.
 * @param name The label name.
 * @param instruction The instruction that this label is for.
 */
data class ExternalLabel(internal val name: String, internal val instruction: Instruction)
