package net.revanced.patcher.signatures

import org.jf.dexlib2.Opcode

/**
 * An ASM signature.
 *
 * ```
 * Signature(
 *    arrayOf(Opcode.ADD_INT),
 *    Modifier.PUBLIC or Modifier.STATIC,
 *    "Ljava/lang/String;"
 * )
 * ```
 *
 * @param opcodes the opcode signature
 * @param attributes the modifiers of the method you are searching for
 * @param returnType the return type of the method as string, see: https://stackoverflow.com/a/9909370
 */
data class Signature(
    val opcodes: Array<Opcode>,
    val attributes: Int,
    val returnType: String
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as Signature

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
