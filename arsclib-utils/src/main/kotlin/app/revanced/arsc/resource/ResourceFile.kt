package app.revanced.arsc.resource

import app.revanced.arsc.ApkResourceException
import app.revanced.arsc.archive.Archive
import app.revanced.arsc.resource.ResourceFile.Handle
import com.reandroid.xml.XMLDocument
import com.reandroid.xml.XMLException
import java.io.*

/**
 * Instantiate a [ResourceFile] and lock the file which [handle] is associated with.
 *
 * @param handle The [Handle] associated with this file.
 * @param archive The [Archive] that the file resides in.
 */
class ResourceFile private constructor(
    internal val handle: Handle,
    private val archive: Archive,
    readResult: Archive.ArchiveResource?
) : Closeable {
    private var pendingWrite = false
    private val isXmlResource = readResult is Archive.ArchiveResource.XmlResource

    init {
        archive.lock(this)
    }

    /**
     * Instantiate a [ResourceFile].
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

    /**
     * @param virtualPath The resource file path. Example: /res/drawable-hdpi/icon.png.
     * @param archivePath The actual file path in the archive. Example: res/4a.png.
     * @param onClose An action to perform when the file associated with this handle is closed
     */
    internal data class Handle(val virtualPath: String, val archivePath: String, val onClose: () -> Unit)

}