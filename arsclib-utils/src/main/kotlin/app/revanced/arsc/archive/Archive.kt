@file:Suppress("MemberVisibilityCanBePrivate")

package app.revanced.arsc.archive

import app.revanced.arsc.ApkResourceException
import app.revanced.arsc.logging.Logger
import app.revanced.arsc.resource.ResourceContainer
import app.revanced.arsc.resource.ResourceFile
import app.revanced.arsc.xml.LazyXMLInputSource
import com.reandroid.apk.ApkModule
import com.reandroid.archive.ByteInputSource
import com.reandroid.arsc.chunk.xml.AndroidManifestBlock
import com.reandroid.arsc.chunk.xml.ResXmlDocument
import com.reandroid.xml.XMLDocument
import java.io.Closeable
import java.io.File

/**
 * A class for reading/writing files in an [ApkModule].
 *
 * @param module The [ApkModule] to operate on.
 */
class Archive(private val module: ApkModule) {
    lateinit var resources: ResourceContainer

    /**
     * The zip archive for the [ApkModule] this [Archive] is operating on.
     */
    private val moduleArchive = module.apkArchive

    private val lockedFiles = mutableMapOf<String, ResourceFile>()

    /**
     * Lock the [ResourceFile], preventing it from being opened again until it is unlocked.
     */
    fun lock(file: ResourceFile) {
        val path = file.handle.archivePath
        if (lockedFiles.contains(path)) {
            throw ApkResourceException.Decode(
                "${file.handle.virtualPath} is currently being used. Close it before opening it again."
            )
        }
        lockedFiles[path] = file
    }

    /**
     * Unlock the [ResourceFile], allowing patches to open it again.
     */
    fun unlock(file: ResourceFile) {
        lockedFiles.remove(file.handle.archivePath)
    }

    /**
     * Closes all open files and encodes all XML files to binary XML.
     *
     * @param logger The [Logger] of the [app.revanced.patcher.Patcher].
     */
    fun cleanup(logger: Logger?) {
        lockedFiles.values.toList().forEach {
            logger?.warn("${it.handle.virtualPath} was never closed!")
            it.close()
        }

        moduleArchive.listInputSources().filterIsInstance<LazyXMLInputSource>()
            .forEach(LazyXMLInputSource::encode)
    }

    /**
     * Save the archive to disk.
     *
     * @param output The file to write the updated archive to.
     */
    fun save(output: File) = module.writeApk(output)

    /**
     * Read an entry from the archive.
     *
     * @param path The archive path to read from.
     * @return A [ArchiveResource] containing the contents of the entry.
     */
    fun read(path: String) = moduleArchive.getInputSource(path)?.let { inputSource ->
        when {
            inputSource is LazyXMLInputSource -> ArchiveResource.XmlResource(inputSource.document)

            ResXmlDocument.isResXmlBlock(inputSource.openStream()) -> ArchiveResource.XmlResource(
                module
                    .loadResXmlDocument(inputSource)
                    .decodeToXml(resources.resourceTable.entryStore, resources.packageBlock?.id ?: 0)
            )

            else -> ArchiveResource.RawResource(inputSource.openStream().use { it.readAllBytes() })
        }
    }

    /**
     * Reads the manifest from the archive as an [AndroidManifestBlock].
     *
     * @return The [AndroidManifestBlock] contained in this archive.
     */
    fun readManifest(): AndroidManifestBlock =
        moduleArchive.getInputSource(AndroidManifestBlock.FILE_NAME).openStream().use { AndroidManifestBlock.load(it) }

    /**
     * Reads all dex files from the archive.
     *
     * @return A [Map] containing all the dex files.
     */
    fun readDexFiles() = module.listDexFiles().associate { file -> file.name to file.openStream() }

    /**
     * Write the byte array to the archive entry.
     *
     * @param path The archive path to read from.
     * @param content The content of the file.
     */
    fun writeRaw(path: String, content: ByteArray) =
        moduleArchive.add(ByteInputSource(content, path))

    /**
     * Write the XML to the entry associated.
     *
     * @param path The archive path to read from.
     * @param document The XML document to encode.
     */
    fun writeXml(path: String, document: XMLDocument) = moduleArchive.add(
        LazyXMLInputSource(
            path,
            document,
            resources,
        )
    )

    /**
     * A resource file of an [Archive].
     */
    abstract class ArchiveResource() : Closeable {
        private var pendingWrite = false

        override fun close() {
            TODO("Not yet implemented")
        }

        /**
         * An [ResXmlDocument] resource file.
         *
         * @param xmlResource The [XMLDocument] of the file.
         */
        class XmlResource(val xmlResource: XMLDocument, archive: Archive) : ArchiveResource()

        /**
         * A raw resource file.
         *
         * @param data The raw data of the file.
         */
        class RawResource(val data: ByteArray, archive: Archive) : ArchiveResource()

        /**
         * @param virtualPath The resource file path. Example: /res/drawable-hdpi/icon.png.
         * @param archivePath The actual file path in the archive. Example: res/4a.png.
         * @param onClose An action to perform when the file associated with this handle is closed
         */
        data class Handle(val virtualPath: String, val archivePath: String, val onClose: () -> Unit)
    }
}