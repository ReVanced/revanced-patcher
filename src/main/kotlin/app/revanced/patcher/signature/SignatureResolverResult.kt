package app.revanced.patcher.signature

import app.revanced.patcher.proxy.ClassProxy
import app.revanced.patcher.resolver.SignatureResolver

data class SignatureResolverResult(
    val definingClassProxy: ClassProxy,
    val resolvedMethodName: String,
    val scanData: PatternScanResult?
) {
    @Suppress("Unused") // TODO(Sculas): remove this when we have coverage for this method.
    fun findParentMethod(signature: MethodSignature): SignatureResolverResult? {
        return SignatureResolver.resolveFromProxy(definingClassProxy, signature)
    }
}

data class PatternScanResult(
    val startIndex: Int,
    val endIndex: Int
)
