package net.revanced.patcher.cache

import org.objectweb.asm.tree.ClassNode
import org.objectweb.asm.tree.MethodNode

data class PatchData(
    val declaringClass: ClassNode,
    val method: MethodNode,
    val scanData: PatternScanData
)

data class PatternScanData(
    val startIndex: Int,
    val endIndex: Int
)
