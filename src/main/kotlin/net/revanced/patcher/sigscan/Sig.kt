package net.revanced.patcher.sigscan

import org.jf.dexlib2.Opcode
import kotlin.reflect.KClass

data class Sig(
    val opcodes: Array<Opcode>,
    val attributes: Int,
    val returnType: KClass<*>
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as Sig

        if (!opcodes.contentEquals(other.opcodes)) return false
        if (attributes != other.attributes) return false
        if (returnType != other.returnType) return false

        return true
    }

    override fun hashCode(): Int {
        var result = opcodes.contentHashCode()
        result = 31 * result + attributes.hashCode()
        result = 31 * result + returnType.hashCode()
        return result
    }
}
