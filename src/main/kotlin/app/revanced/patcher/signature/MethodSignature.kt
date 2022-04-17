package app.revanced.patcher.signature

import app.revanced.patcher.MethodNotFoundException
import org.jf.dexlib2.Opcode

/**
 * Represents the [MethodSignature] for a method.
 * @param metadata Metadata for this [MethodSignature].
 * @param returnType The return type of the method.
 * @param accessFlags The access flags of the method.
 * @param methodParameters The parameters of the method.
 * @param opcodes The list of opcodes of the method.
 * @param strings A list of strings which a method contains.
 * A `null` opcode is equals to an unknown opcode.
 */
class MethodSignature(
    val metadata: MethodSignatureMetadata,
    internal val returnType: String?,
    internal val accessFlags: Int?,
    internal val methodParameters: Iterable<String>?,
    internal val opcodes: Iterable<Opcode?>?,
    internal val strings: Iterable<String>?
) {
    /**
     * The result of the signature
     */
    var result: SignatureResolverResult? = null
        get() {
            return field ?: throw MethodNotFoundException(
                "Could not resolve required signature ${metadata.name}"
            )
        }
    val resolved: Boolean
        get() {
            var resolved = false
            try {
                resolved = result != null
            } catch (_: Exception) {}
            return resolved
        }
}

/**
 * Metadata about a [MethodSignature].
 * @param name A suggestive name for the [MethodSignature].
 * @param methodMetadata Metadata about the method for the [MethodSignature].
 * @param patternScanMethod The pattern scanning method the pattern scanner should rely on.
 * Can either be [PatternScanMethod.Fuzzy] or [PatternScanMethod.Direct].
 * @param description An optional description of the [MethodSignature].
 * @param compatiblePackages The list of packages the [MethodSignature] is compatible with.
 * @param version The version of this signature.
 */
data class MethodSignatureMetadata(
    val name: String,
    val methodMetadata: MethodMetadata,
    val patternScanMethod: PatternScanMethod,
    val compatiblePackages: Iterable<String>,
    val description: String?,
    val version: String
)

/**
 * Metadata about the method for a [MethodSignature].
 * @param definingClass The defining class name of the method.
 * @param name A suggestive name for the method which the [MethodSignature] was created for.
 */
data class MethodMetadata(
    val definingClass: String?,
    val name: String?
)

/**
 * The method, the patcher should rely on when scanning the opcode pattern of a [MethodSignature]
 */
interface PatternScanMethod {
    /**
     * When comparing the signature, if one or more of the opcodes do not match, skip.
     */
    class Direct : PatternScanMethod

    /**
     * When comparing the signature, if [threshold] or more of the opcodes do not match, skip.
     */
    class Fuzzy(internal val threshold: Int) : PatternScanMethod {
        /**
         * A list of warnings the resolver found.
         *
         * This list will be allocated when the signature has been found.
         * Meaning, if the signature was not found,
         * or the signature was not yet resolved,
         * the list will be null.
         */
        var warnings: List<Warning>? = null

        /**
         * Represents a resolver warning.
         * @param correctOpcode The opcode the signature expected it to be.
         * @param wrongOpcode The opcode the signature currently has.
         * @param instructionIndex The index of the opcode relative to the instruction list.
         * @param patternIndex The index of the opcode relative to the pattern list from the signature.
         */
        data class Warning(
            val correctOpcode: Opcode,
            val wrongOpcode: Opcode,
            val instructionIndex: Int,
            val patternIndex: Int,
        )
    }
}