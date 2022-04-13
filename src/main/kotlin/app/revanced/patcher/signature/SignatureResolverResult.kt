package app.revanced.patcher.signature

import app.revanced.patcher.proxy.ClassProxy
import app.revanced.patcher.signature.resolver.SignatureResolver
import org.jf.dexlib2.iface.Method

/**
 * Represents the result of a [SignatureResolver].
 * @param definingClassProxy The [ClassProxy] that the matching method was found in.
 * @param resolvedMethodName The name of the actual matching method.
 * @param scanData Opcodes pattern scan result.
 */
data class SignatureResolverResult(
    val definingClassProxy: ClassProxy,
    val scanData: PatternScanResult,
    private val resolvedMethodName: String,
) {
    /**
     * Returns the **mutable** method by the [resolvedMethodName] from the [definingClassProxy].
     *
     * Please note, this method creates a [ClassProxy].
     * Use [immutableMethod] where possible.
     */
    val method
        get() = definingClassProxy.resolve().methods.first {
            it.name == resolvedMethodName
        }

    /**
     * Returns the **immutable** method by the [resolvedMethodName] from the [definingClassProxy].
     *
     * If you need to modify the method, use [method] instead.
     */
    val immutableMethod: Method
        get() = definingClassProxy.immutableClass.methods.first {
            it.name == resolvedMethodName
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
