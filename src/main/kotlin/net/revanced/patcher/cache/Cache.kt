package net.revanced.patcher.cache

data class Cache(
    val methods: MethodMap = MethodMap()
)

class MethodMap : LinkedHashMap<String, PatchData>() {
    override fun get(key: String): PatchData {
        return super.get(key) ?: throw MethodNotFoundException("Method $key not found in method cache")
    }
}

class MethodNotFoundException(s: String) : Exception(s)
