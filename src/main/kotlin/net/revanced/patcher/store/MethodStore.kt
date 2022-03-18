package net.revanced.patcher.store

import org.objectweb.asm.tree.MethodNode

class MethodStore {
    val methods: MutableMap<String, MethodNode> = mutableMapOf()
}