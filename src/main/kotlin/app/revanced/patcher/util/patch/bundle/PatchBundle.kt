package app.revanced.patcher.util.patch.bundle

import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.util.zip.Deflater
import java.util.zip.Inflater

data class PatchBundle(val metadata: Metadata, val resources: PatchResourceContainer) {
    data class Metadata(
        val name: String,
        val version: String,
        val authors: String
    )
}

class PatchBundleFormat {
    companion object {
        @JvmStatic
        fun serialize(metadata: PatchBundle.Metadata, resources: List<PatchResource>): ByteArray {
            val buf = ByteArrayOutputStream()
            buf.writeString(metadata.name)
            buf.writeString(metadata.version)
            buf.writeString(metadata.authors)
            buf.write(resources.size)

            if (resources.isNotEmpty()) {
                val deflater = Deflater()
                for ((key, resource) in resources) {
                    val compressedResource = deflater.compress(resource)
                    val compressedSize = compressedResource.size

                    buf.writeString(key)
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
                        val compressedSize = buf.readChecked()
                        val compressedResource = buf.readNBytes(compressedSize)
                        val resource = inflater.decompress(compressedResource)

                        put(key, resource)
                    }
                }
                inflater.end()
                map
            } else emptyMap()

            return PatchBundle(
                PatchBundle.Metadata(name, version, authors), PatchResourceContainer(resources)
            )
        }
    }
}