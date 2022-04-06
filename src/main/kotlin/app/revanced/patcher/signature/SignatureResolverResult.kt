package app.revanced.patcher.signature

import app.revanced.patcher.proxy.ClassProxy
import app.revanced.patcher.proxy.mutableTypes.MutableMethod
import app.revanced.patcher.resolver.SignatureResolver

/**
 * Represents the result of a [SignatureResolver].
 * @param definingClassProxy The [ClassProxy] that the matching method was found in.
 * @param resolvedMethodName The name of the actual matching method.
 * @param scanData OpCodes pattern scan result.
 */
data class SignatureResolverResult(
    val definingClassProxy: ClassProxy,
    val scanData: PatternScanResult,
    private val resolvedMethodName: String,
) {

    fun resolveAndGetMethod(): MutableMethod {
        return definingClassProxy.resolve().methods.single { it.name == resolvedMethodName }
    }

    @Suppress("Unused") // TODO(Sculas): remove this when we have coverage for this method.
    fun findParentMethod(signature: MethodSignature): SignatureResolverResult? {
        return SignatureResolver.resolveFromProxy(definingClassProxy, signature)
    }
}

data class PatternScanResult(
    val startIndex: Int,
    val endIndex: Int
)
