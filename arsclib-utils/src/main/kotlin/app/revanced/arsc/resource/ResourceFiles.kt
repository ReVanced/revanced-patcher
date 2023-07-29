package app.revanced.arsc.resource

import app.revanced.arsc.ApkResourceException
import app.revanced.arsc.archive.Archive
import com.reandroid.archive.InputSource
import com.reandroid.xml.XMLDocument
import com.reandroid.xml.XMLException
import java.io.*


abstract class ResourceFile(val name: String) {
    internal var realName: String? = null

    class XmlResourceFile(name: String, val document: XMLDocument) : ResourceFile(name)
    class BinaryResourceFile(name: String, var bytes: ByteArray) : ResourceFile(name)
}


class ResourceFiles private constructor(
) : Closeable {

    /**
     * Instantiate a [ResourceFiles].
     *
     * @param handle The [Handle] associated with this file.
     * @param archive The [Archive] that the file resides in.
     */
    internal constructor(handle: Handle, archive: Archive) : this(
        handle,
        archive,
        try {
            archive.read(handle.archivePath)
        } catch (e: XMLException) {
            throw ApkResourceException.Decode("Failed to decode XML while reading ${handle.virtualPath}", e)
        } catch (e: IOException) {
            throw ApkResourceException.Decode("Could not read ${handle.virtualPath}", e)
        }
    )

    companion object {
        const val DEFAULT_BUFFER_SIZE = 1024
    }

    var contents = readResult?.data ?: ByteArray(0)
        set(value) {
            pendingWrite = true
            field = value
        }

    val exists = readResult != null

    override fun toString() = handle.virtualPath

    override fun close() {
        if (pendingWrite) {
            val path = handle.archivePath

            if (isXmlResource) archive.writeXml(
                path,
                try {
                    XMLDocument.load(inputStream())
                } catch (e: XMLException) {
                    throw ApkResourceException.Encode("Failed to parse XML while writing ${handle.virtualPath}", e)
                }

            ) else archive.writeRaw(path, contents)
        }

        handle.onClose()


        archive.unlock(this)
    }

    fun inputStream(): InputStream = ByteArrayInputStream(contents)

    fun outputStream(bufferSize: Int = DEFAULT_BUFFER_SIZE): OutputStream =
        object : ByteArrayOutputStream(bufferSize) {
            override fun close() {
                this@ResourceFiles.contents = if (buf.size > count) buf.copyOf(count) else buf
                super.close()
            }
        }

    /**
     * @param virtualPath The resource file path. Example: /res/drawable-hdpi/icon.png.
     * @param archivePath The actual file path in the archive. Example: res/4a.png.
     * @param onClose An action to perform when the file associated with this handle is closed
     */
    internal data class Handle(val virtualPath: String, val archivePath: String, val onClose: () -> Unit)
}