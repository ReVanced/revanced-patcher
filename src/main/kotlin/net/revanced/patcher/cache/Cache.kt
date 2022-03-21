package net.revanced.patcher.cache

import org.objectweb.asm.tree.ClassNode

class Cache(
    val classes: List<ClassNode>,
    val methods: MethodMap
)

class MethodMap : LinkedHashMap<String, PatchData>() {
    override fun get(key: String): PatchData {
        return super.get(key) ?: throw MethodNotFoundException("Method $key was not found in the method cache")
    }
}

class MethodNotFoundException(s: String) : Exception(s)
