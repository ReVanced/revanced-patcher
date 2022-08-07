package app.revanced.patcher.util.patch.bundle

import java.io.File
import java.io.InputStream

data class PatchResource(val key: String, val type: PatchResourceType, val data: ByteArray) {
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

    companion object {
        fun fromFile(file: File, type: PatchResourceType) = PatchResource(file.name, type, file.readBytes())
    }
}

enum class PatchResourceType(val id: Byte) {
    RESOURCE(0x1),
    JAR(0x2),
    DEX(0x2);

    companion object {
        fun isValid(id: Byte) = values().any { it.id == id }

        operator fun get(id: Byte) = values().first { it.id == id }
    }
}

// not using List<PatchResource> here for performance reasons.
open class PatchResourceContainer(private val resources: List<PatchResource>) : Iterable<PatchResource> {
    private val register = buildMap { resources.forEach { put(it.key, it) } }
    val size = resources.size

    operator fun get(path: String): PatchResource? = register[path]

    fun streamOf(path: String): InputStream? = get(path)?.data?.inputStream()

    override fun iterator() = resources.iterator()
}