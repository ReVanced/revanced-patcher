package app.revanced.patcher.signature

import org.jf.dexlib2.Opcode

/**
 * Represents a method signature.
 * @param name A suggestive name for the method which the signature was created for.
 * @param returnType The return type of the method.
 * @param methodParameters The parameters of the method.
 * @param opcodes A list of opcodes of the method.
 * @param accessFlags The access flags of the method.
 */
@Suppress("ArrayInDataClass")
data class MethodSignature(
    val name: String,
    val returnType: String?,
    val accessFlags: Int?,
    val methodParameters: Array<String>?,
    val opcodes: Array<Opcode>?
)