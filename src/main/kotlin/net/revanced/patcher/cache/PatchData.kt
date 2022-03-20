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
    fun findParentMethod(signature: Signature): MethodMap {
       return MethodResolver(listOf(declaringClass), arrayOf(signature)).resolve()
    }
}

data class PatternScanData(
    val startIndex: Int,
    val endIndex: Int
)
