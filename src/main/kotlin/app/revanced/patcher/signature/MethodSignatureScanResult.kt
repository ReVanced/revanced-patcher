package app.revanced.patcher.signature

import app.revanced.patcher.resolver.MethodResolver
import org.jf.dexlib2.iface.ClassDef
import org.jf.dexlib2.iface.Method
import org.jf.dexlib2.immutable.reference.ImmutableTypeReference

// TODO: IMPORTANT: we might have to use a class proxy as well here
data class MethodSignatureScanResult(
    val method: Method,
    val scanData: PatternScanData
) {
    @Suppress("Unused") // TODO(Sculas): remove this when we have coverage for this method.
    fun findParentMethod(signature: MethodSignature): MethodSignatureScanResult? {
        // TODO: find a way to get the classNode out of method.definingClass
        return MethodResolver.resolveMethod(ImmutableTypeReference(method.definingClass) as ClassDef, signature)
    }
}

data class PatternScanData(
    val startIndex: Int,
    val endIndex: Int
)
