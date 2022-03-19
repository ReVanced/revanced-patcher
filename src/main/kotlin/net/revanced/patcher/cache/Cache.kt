package net.revanced.patcher.cache

import org.objectweb.asm.tree.MethodNode

data class Cache(
    var Methods: Map<String, MethodNode> = mutableMapOf()
)
