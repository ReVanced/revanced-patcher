package app.revanced.patcher.util.patch.bundle

import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.InputStream
import java.util.zip.Deflater
import java.util.zip.Inflater

class PatchBundleFormat {
    companion object {
        @JvmStatic
        fun serialize(bundle: PatchBundle): ByteArray {
            val buf = ByteArrayOutputStream()
            buf.writeString(bundle.name)
            buf.writeString(bundle.version)
            buf.writeString(bundle.authors)
            buf.write(bundle.resources.size)

            val deflater = Deflater()
            for ((key, resource) in bundle.resources.all()) {
                if (resource.available() < 1) continue
                buf.writeString(key)

                deflater.setInput(resource.readAllBytes())
                deflater.finish()

                val compressedResource = ByteArray(1024)
                val compressedSize = deflater.deflate(compressedResource)

                buf.write(compressedSize)
                buf.write(compressedResource)
            }
            deflater.end()

            return buf.toByteArray()
        }

        @JvmStatic
        fun deserialize(bytes: ByteArray): PatchBundle {
            val buf = ByteArrayInputStream(bytes)
            val name = buf.readString()
            val version = buf.readString()
            val authors = buf.readString()

            val inflater = Inflater()
            val resources = buildMap {
                for (i in 0 until buf.read()) {
                    val key = buf.readString()
                    val compressedResource = buf.readNBytes(buf.read())

                    inflater.setInput(compressedResource)

                    val resource = ByteArray(1024)
                    inflater.inflate(resource)

                    put(key, resource)
                }
            }
            inflater.end()

            return PatchBundle(name, version, authors, FormatResourceContainer(resources))
        }
    }
}

data class PatchBundle(
    val name: String,
    val version: String,
    val authors: String,
    val resources: ResourceContainer
)

interface ResourceContainer {
    val size: Int
    operator fun get(path: String): InputStream?
    fun all(): Map<String, InputStream>
}

// TODO: how can I get all resources?
class JVMResourceContainer : ResourceContainer {
    override val size = 0
    override fun get(path: String): InputStream? = javaClass.classLoader.getResourceAsStream(path)
    override fun all(): Map<String, InputStream> {
        TODO("Not yet implemented")
    }
}

// TODO: give this a better name
class FormatResourceContainer(private val resources: Map<String, ByteArray>) : ResourceContainer {
    override val size = resources.size
    override fun get(path: String): InputStream = resources[path]!!.inputStream()
    override fun all() = resources.mapValues { it.value.inputStream() }
}

fun ByteArrayOutputStream.writeString(str: String) {
    write(str.length)
    write(str.toByteArray())
}

fun ByteArrayInputStream.readString() = String(readNBytes(read()))