package net.revanced.patcher.signature

import org.objectweb.asm.Type

/**
 * An ASM signature list for the Patcher.
 *
 * @param name The name of the method.
 * Do not use the actual method name, instead try to guess what the method name originally was.
 * If you are unable to guess a method name, doing something like "patch-name-1" is fine too.
 * For example: "override-codec-1".
 * This method name will be used to find the corresponding patch.
 * @param returns The return type/signature of the method.
 * @param accessors The accessors of the method.
 * @param parameters The parameter types/signatures of the method.
 * @param opcodes The opcode pattern of the method, used to find the method by signature scanning.
 */
data class Signature(
    val name: String,
    val returns: Type,
    val accessors: Array<Int>,
    val parameters: Array<Type>,
    val opcodes: Array<Int>
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as Signature

        if (name != other.name) return false
        if (returns != other.returns) return false
        if (!accessors.contentEquals(other.accessors)) return false
        if (!parameters.contentEquals(other.parameters)) return false
        if (!opcodes.contentEquals(other.opcodes)) return false

        return true
    }

    override fun hashCode(): Int {
        var result = name.hashCode()
        result = 31 * result + returns.hashCode()
        result = 31 * result + accessors.contentHashCode()
        result = 31 * result + parameters.contentHashCode()
        result = 31 * result + opcodes.contentHashCode()
        return result
    }
}
