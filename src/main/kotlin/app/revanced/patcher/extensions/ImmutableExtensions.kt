package app.revanced.patcher.extensions

import org.jf.dexlib2.iface.instruction.ReferenceInstruction
import org.jf.dexlib2.iface.reference.MethodReference
import org.jf.dexlib2.immutable.reference.ImmutableMethodReference
import java.util.*

object ImmutableExtensions {
    enum class MethodReferenceMatch {
        DEFINING_CLASS, NAME, PARAMTER_TYPES, RETURN_TYPE
    }

    fun ImmutableMethodReference.matches(
        instruction: ReferenceInstruction,
        match: EnumSet<MethodReferenceMatch> = EnumSet.allOf(MethodReferenceMatch::class.java)
    ): Boolean {
        val ref = (instruction.reference as? MethodReference) ?: return false

        if (match.contains(MethodReferenceMatch.DEFINING_CLASS) && ref.definingClass != definingClass) return false
        if (match.contains(MethodReferenceMatch.NAME) && ref.name != name) return false
        if (match.contains(MethodReferenceMatch.PARAMTER_TYPES) && ref.parameterTypes != parameterTypes) return false
        if (match.contains(MethodReferenceMatch.RETURN_TYPE) && ref.returnType != returnType) return false

        return true
    }
}
