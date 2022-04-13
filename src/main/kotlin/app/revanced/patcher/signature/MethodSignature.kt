package app.revanced.patcher.signature

import org.jf.dexlib2.Opcode

/**
 * Represents a method signature.
 * @param name A suggestive name for the method which the signature was created for.
 * @param metadata Metadata about this signature.
 * @param returnType The return type of the method.
 * @param accessFlags The access flags of the method.
 * @param methodParameters The parameters of the method.
 * @param opcodes A list of opcodes of the method.
 */
@Suppress("ArrayInDataClass")
data class MethodSignature(
    val name: String,
    val metadata: SignatureMetadata,
    val returnType: String?,
    val accessFlags: Int?,
    val methodParameters: Iterable<String>?,
    val opcodes: Iterable<Opcode>?
)

/**
 * Metadata about the signature.
 * @param method Metadata about the method for this signature.
 * @param patcher Metadata for the Patcher, this contains things like how the Patcher should interpret this signature.
 */
data class SignatureMetadata(
    val method: MethodMetadata,
    val patcher: PatcherMetadata
)

/**
 * Metadata about the method for this signature.
 * @param definingClass The defining class name of the original method.
 * @param methodName The name of the original method.
 * @param comment A comment about this method and the data above.
 * For example, the version this signature was originally made for.
 */
data class MethodMetadata(
    val definingClass: String?,
    val methodName: String?,
    val comment: String
)

/**
 * Metadata for the Patcher, this contains things like how the Patcher should interpret this signature.
 * @param method The method the Patcher should use to resolve the signature.
 */
data class PatcherMetadata(
    val method: PatcherMethod
)

interface PatcherMethod {
    /**
     * When comparing the signature, if one or more of the opcodes do not match, skip.
     */
    class Direct : PatcherMethod

    /**
     * When comparing the signature, if [threshold] or more of the opcodes do not match, skip.
     */
    class Fuzzy(val threshold: Int) : PatcherMethod
}