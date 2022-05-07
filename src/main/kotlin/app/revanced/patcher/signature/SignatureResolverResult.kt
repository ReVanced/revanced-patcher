package app.revanced.patcher.signature

import app.revanced.patcher.extensions.softCompareTo
import app.revanced.patcher.proxy.ClassProxy
import app.revanced.patcher.signature.resolver.SignatureResolver
import org.jf.dexlib2.iface.Method

/**
 * Represents the result of a [SignatureResolver].
 * @param definingClassProxy The [ClassProxy] that the matching method was found in.
 * @param resolvedMethod The actual matching method.
 * @param scanData Opcodes pattern scan result.
 */
data class SignatureResolverResult(
    val definingClassProxy: ClassProxy,
    val scanData: PatternScanResult,
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
    val immutableMethod: Method
        get() = definingClassProxy.immutableClass.methods.first {
            it.softCompareTo(resolvedMethod)
        }

    fun findParentMethod(signature: MethodSignature): SignatureResolverResult? {
        return SignatureResolver.resolveFromProxy(definingClassProxy, signature)
    }
}

data class PatternScanResult(
    val startIndex: Int,
    val endIndex: Int
)
