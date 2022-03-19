package net.revanced.patcher.cache

import org.objectweb.asm.tree.ClassNode
import org.objectweb.asm.tree.MethodNode

data class PatchData(
    val cls: ClassNode,
    val method: MethodNode
)
