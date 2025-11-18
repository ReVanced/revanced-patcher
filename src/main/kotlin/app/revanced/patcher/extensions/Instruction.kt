package app.revanced.patcher.extensions

import com.android.tools.smali.dexlib2.iface.instruction.DualReferenceInstruction
import com.android.tools.smali.dexlib2.iface.instruction.Instruction
import com.android.tools.smali.dexlib2.iface.instruction.ReferenceInstruction
import com.android.tools.smali.dexlib2.iface.reference.Reference

@Suppress("UNCHECKED_CAST")
fun <T : Reference> Instruction.reference(predicate: T.() -> Boolean) =
    ((this as? ReferenceInstruction)?.reference as? T)?.predicate() ?: false

@Suppress("UNCHECKED_CAST")
fun <T : Reference> Instruction.reference2(predicate: T.() -> Boolean) =
    ((this as? DualReferenceInstruction)?.reference2 as? T)?.predicate() ?: false
