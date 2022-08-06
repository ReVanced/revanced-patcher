package app.revanced.patcher.util.patch.bundle

import java.io.InputStream

data class PatchResource(val key: String, val data: ByteArray) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as PatchResource

        if (key != other.key) return false
        if (!data.contentEquals(other.data)) return false

        return true
    }

    override fun hashCode(): Int {
        var result = key.hashCode()
        result = 31 * result + data.contentHashCode()
        return result
    }
}

// not using List<PatchResource> here for performance reasons.
open class PatchResourceContainer(private val resources: Map<String, ByteArray>) : Iterable<PatchResource> {
    private val cachedResources = resources.map { PatchResource(it.key, it.value) }

    val size = resources.size
    operator fun get(path: String): InputStream = resources[path]!!.inputStream()
    override fun iterator() = cachedResources.iterator()
}