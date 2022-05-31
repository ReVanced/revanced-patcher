package app.revanced.patcher.signature.implementation.method

import app.revanced.patcher.data.implementation.MethodNotFoundException
import app.revanced.patcher.extensions.MethodSignatureExtensions.name
import app.revanced.patcher.signature.base.Signature
import app.revanced.patcher.signature.implementation.method.resolver.SignatureResolverResult
import org.jf.dexlib2.Opcode

/**
 * Represents the [MethodSignature] for a method.
 * @param returnType The return type of the method.
 * @param accessFlags The access flags of the method.
 * @param methodParameters The parameters of the method.
 * @param opcodes The list of opcodes of the method.
 * @param strings A list of strings which a method contains.
 * A `null` opcode is equals to an unknown opcode.
 */
abstract class MethodSignature(
    internal val returnType: String?,
    internal val accessFlags: Int?,
    internal val methodParameters: Iterable<String>?,
    internal val opcodes: Iterable<Opcode?>?,
    internal val strings: Iterable<String>? = null
) : Signature {
    /**
     * The result of the signature
     */
    var result: SignatureResolverResult? = null
        @Throws(MethodNotFoundException::class)
        get() {
            return field ?: throw MethodNotFoundException("Could not resolve required signature ${this.name}")
        }
}