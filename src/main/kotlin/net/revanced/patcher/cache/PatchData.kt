package net.revanced.patcher.cache

import net.revanced.patcher.resolver.MethodResolver
import net.revanced.patcher.signature.Signature
import org.objectweb.asm.tree.ClassNode
import org.objectweb.asm.tree.MethodNode

data class PatchData(
    val declaringClass: ClassNode,
    val method: MethodNode,
    val scanData: PatternScanData
) {
    @Suppress("Unused") // TODO(Sculas): remove this when we have coverage for this method.
    fun findParentMethod(signature: Signature): PatchData? {
        return MethodResolver.resolveMethod(declaringClass, signature)
    }
}

data class PatternScanData(
    val startIndex: Int,
    val endIndex: Int
)
