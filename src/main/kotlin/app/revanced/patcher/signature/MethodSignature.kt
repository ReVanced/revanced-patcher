package app.revanced.patcher.signature

import org.jf.dexlib2.Opcode

@Suppress("ArrayInDataClass")
data class MethodSignature(
    val name: String,
    val returnType: String?,
    val accessFlags: Int?,
    val methodParameters: Iterable<CharSequence>?,
    val opcodes: Array<Opcode>?
)