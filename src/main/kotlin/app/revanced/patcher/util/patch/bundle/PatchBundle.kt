package app.revanced.patcher.util.patch.bundle

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
        return ByteArrayOutputStream().use { buf ->
            ZipOutputStream(buf).use { zip ->
                zip.setComment(Header.write(metadata))

                for (resource in resources) {
                    zip.putNextEntry(ZipEntry(resource.key).apply {
                        extra = byteArrayOf(resource.type.id)
                    })
                    zip.write(resource.data)
                    zip.closeEntry()
                }
            }
            buf.toByteArray()
        }
    }

    @JvmStatic
    // FIXME: to the one who wants to fix this, don't use a File.
    //  This implementation must be done in memory.
    //  Won't merge it otherwise.
    fun deserialize(bytes: ByteArray): PatchBundle {
        return PatchBundle(
            PatchBundle.Metadata(name, version, authors),
            PatchResourceContainer(resources)
        )
    }

    private object Header {
        @JvmStatic
        fun write(metadata: PatchBundle.Metadata): String =
            ByteArrayOutputStream(MAGIC_LEN + metadata.size).use { buf ->
                buf.writeBytes(MAGIC)
                buf.writeString(metadata.name)
                buf.writeString(metadata.version)
                buf.writeString(metadata.authors)

                buf.toString(Charsets.UTF_8)
            }

        @JvmStatic
        fun read(s: String): PatchBundle.Metadata =
            s.byteInputStream(Charsets.UTF_8).use { buf ->
                buf.readNBytesAssert(MAGIC_LEN) { it.contentEquals(MAGIC) }

                val name = buf.readString()
                val version = buf.readString()
                val authors = buf.readString()

                PatchBundle.Metadata(name, version, authors)
            }
    }
}