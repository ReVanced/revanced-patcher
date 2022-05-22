package app.revanced.patcher.signature.implementation.method.resolver

import app.revanced.patcher.extensions.softCompareTo
import app.revanced.patcher.signature.implementation.method.MethodSignature
import app.revanced.patcher.util.proxy.ClassProxy
import org.jf.dexlib2.Opcode
import org.jf.dexlib2.iface.Method

/**
 * Represents the result of a [MethodSignatureResolver].
 * @param definingClassProxy The [ClassProxy] that the matching method was found in.
 * @param resolvedMethod The actual matching method.
 * @param scanResult Opcodes pattern scan result.
 */
data class SignatureResolverResult(
    val definingClassProxy: ClassProxy,
    val scanResult: PatternScanResult,
    private val resolvedMethod: Method,
) {
    /**
     * Returns the **mutable** method by the [resolvedMethod] from the [definingClassProxy].
     *
     * Please note, this method allocates a [ClassProxy].
     * Use [immutableMethod] where possible.
     */
    val method
        get() = definingClassProxy.resolve().methods.first {
            it.softCompareTo(resolvedMethod)
        }

    /**
     * Returns the **immutable** method by the [resolvedMethod] from the [definingClassProxy].
     *
     * If you need to modify the method, use [method] instead.
     */
    @Suppress("MemberVisibilityCanBePrivate")
    val immutableMethod: Method
        get() = definingClassProxy.immutableClass.methods.first {
            it.softCompareTo(resolvedMethod)
        }

    fun findParentMethod(signature: MethodSignature): SignatureResolverResult? {
        return MethodSignatureResolver.resolveFromProxy(definingClassProxy, signature)
    }
}

data class PatternScanResult(
    val startIndex: Int,
    val endIndex: Int
) {
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
     * @param correctOpcode The opcode the instruction list has.
     * @param wrongOpcode The opcode the pattern list of the signature currently has.
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

