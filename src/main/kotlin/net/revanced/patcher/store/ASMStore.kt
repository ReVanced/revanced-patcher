package net.revanced.patcher.store

import org.objectweb.asm.tree.ClassNode
import org.objectweb.asm.tree.MethodNode

class ASMStore {
    val classes: MutableMap<String, ClassNode> = mutableMapOf()
    val methods: MutableMap<String, MethodNode> = mutableMapOf()
}