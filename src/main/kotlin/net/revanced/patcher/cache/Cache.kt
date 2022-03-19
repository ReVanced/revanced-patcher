package net.revanced.patcher.cache

import org.objectweb.asm.tree.ClassNode

class Cache {
    val classes: MutableMap<String, ClassNode> = mutableMapOf()
    val methods: MethodMap = MethodMap()
}

class MethodMap : LinkedHashMap<String, PatchData>() {
    override fun get(key: String): PatchData {
        return super.get(key) ?: throw MethodNotFoundException("Method $key not found in method cache")
    }
}

class MethodNotFoundException(s: String) : Exception(s)
