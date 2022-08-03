package app.revanced.patcher.util.patch.bundle

import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.InputStream
import java.util.zip.Deflater
import java.util.zip.Inflater

class PatchBundleFormat {
    companion object {
        @JvmStatic
        fun serialize(bundle: PatchBundle.Metadata, resources: Map<String, ByteArray>): ByteArray {
            val buf = ByteArrayOutputStream()
            buf.writeString(bundle.name)
            buf.writeString(bundle.version)
            buf.writeString(bundle.authors)
            buf.write(resources.size)

            if (resources.isNotEmpty()) {
                val deflater = Deflater()
                for ((key, resource) in resources) {
                    if (resource.isEmpty()) continue
                    val size = resource.size
                    buf.writeString(key)

                    deflater.setInput(resource)
                    deflater.finish()

                    val compressedResource = ByteArray(size)
                    val compressedSize = deflater.deflate(compressedResource)

                    buf.write(size)
                    buf.write(compressedSize)
                    buf.write(compressedResource)
                }
                deflater.end()
            }

            return buf.toByteArray()
        }

        @JvmStatic
        fun deserialize(bytes: ByteArray): PatchBundle {
            val buf = ByteArrayInputStream(bytes)
            val name = buf.readString()
            val version = buf.readString()
            val authors = buf.readString()

            val resources = if (buf.available() > 0) {
                val inflater = Inflater()
                val map = buildMap {
                    for (i in 0 until buf.read()) {
                        val key = buf.readString()
                        val size = buf.read().also(::checkSize)

                        val compressedResource = buf.readNBytes(buf.read().also(::checkSize))
                        inflater.setInput(compressedResource)

                        val resource = ByteArray(size)
                        inflater.inflate(resource)

                        put(key, resource)
                    }
                }
                inflater.end()
                map
            } else emptyMap()

            return PatchBundle(PatchBundle.Metadata(name, version, authors), PatchResourceContainer(resources))
        }
    }
}

fun checkSize(size: Int) {
    if (size > Int.MAX_VALUE) throw IllegalStateException("Resource size is stupidly high, malicious bundle?")
    if (size < 1) throw IllegalStateException("Malformed resource size")
}

data class PatchBundle(
    val metadata: Metadata,
    val resources: PatchResourceContainer
) {
    data class Metadata(
        val name: String,
        val version: String,
        val authors: String
    )
}

@Suppress("ArrayInDataClass")
data class PatchResource(val key: String, val data: ByteArray)

class PatchResourceContainer(private val resources: Map<String, ByteArray>) : Iterable<PatchResource> {
    private val cachedResources = resources.map { PatchResource(it.key, it.value) }

    val size = resources.size
    fun get(path: String): InputStream = resources[path]!!.inputStream()
    override fun iterator() = cachedResources.iterator()
}

fun ByteArrayOutputStream.writeString(str: String) {
    write(str.length)
    write(str.toByteArray())
}

fun ByteArrayInputStream.readString() = String(readNBytes(read()))