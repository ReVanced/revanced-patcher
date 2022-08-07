package app.revanced.patcher.util.patch.bundle

import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.util.zip.*

data class PatchBundle(val metadata: Metadata, val resources: PatchResourceContainer) {
    data class Metadata(
        val name: String,
        val version: String,
        val authors: String
    ) {
        val size = name.length + version.length + authors.length
    }
}

object PatchBundleFormat {
    val MAGIC = byteArrayOf(0x72, 0x76) // "rv"
    private val MAGIC_LEN = MAGIC.size

    @JvmStatic
    fun serialize(metadata: PatchBundle.Metadata, resources: List<PatchResource>): ByteArray {
        val buf = ByteArrayOutputStream()
        buf.writeBytes(MAGIC)

        buf.writeString(metadata.name)
        buf.writeString(metadata.version)
        buf.writeString(metadata.authors)
        buf.write(resources.size)

        if (resources.isNotEmpty()) {
            val deflater = Deflater()
            for ((key, type, resource) in resources) {
                val compressedResource = deflater.compress(resource)
                val compressedSize = compressedResource.size

                buf.writeString(key)
                buf.write(type.id)
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
        buf.readNBytesAssert(MAGIC_LEN) { it.contentEquals(MAGIC) }

        val name = buf.readString()
        val version = buf.readString()
        val authors = buf.readString()

        val resources = if (buf.available() > 0) {
            val inflater = Inflater()
            val list = buildList {
                for (i in 0 until buf.readChecked()) {
                    val key = buf.readString()
                    val type = PatchResourceType[buf.readChecked(PatchResourceType::isValid)]
                    val compressedSize = buf.readChecked { it > 0 }
                    val compressedResource = buf.readNBytesAssert(compressedSize)
                    val resource = inflater.decompress(compressedResource)

                    add(PatchResource(key, type, resource))
                }
            }
            inflater.end()
            list
        } else listOf()

        return PatchBundle(
            PatchBundle.Metadata(name, version, authors),
            PatchResourceContainer(resources)
        )
    }
}