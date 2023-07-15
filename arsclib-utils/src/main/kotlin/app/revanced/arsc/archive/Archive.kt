package app.revanced.arsc.archive

import app.revanced.arsc.ApkException
import app.revanced.arsc.logging.Logger
import app.revanced.arsc.resource.ResourceContainer
import app.revanced.arsc.resource.ResourceFile
import app.revanced.arsc.xml.LazyXMLInputSource
import com.reandroid.apk.ApkModule
import com.reandroid.archive.ByteInputSource
import com.reandroid.archive.InputSource
import com.reandroid.arsc.chunk.xml.AndroidManifestBlock
import com.reandroid.arsc.chunk.xml.ResXmlDocument
import com.reandroid.xml.XMLDocument
import java.io.File

private fun isResXml(inputSource: InputSource) = inputSource.openStream().use { ResXmlDocument.isResXmlBlock(it) }

/**
 * A class for reading/writing files in an [ApkModule].
 *
 * @param module The [ApkModule] to operate on.
 */
class Archive(private val module: ApkModule) {
    lateinit var resources: ResourceContainer

    /**
     * The result of a [read] operation.
     *
     * @param xml Whether the contents were decoded from a [ResXmlDocument].
     * @param data The contents of the file.
     */
    class ReadResult(val xml: Boolean, val data: ByteArray)

    /**
     * The zip archive.
     */
    private val archive = module.apkArchive

    private val lockedFiles = mutableMapOf<String, ResourceFile>()

    /**
     * Lock the [ResourceFile], preventing it from being opened again until it is unlocked.
     */
    fun lock(file: ResourceFile) {
        val path = file.handle.archivePath
        if (lockedFiles.contains(path)) {
            throw ApkException.Decode("${file.handle.virtualPath} is locked. If you are a patch developer, make sure you always close files.")
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

        archive.listInputSources().filterIsInstance<LazyXMLInputSource>()
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
     * @return A [ReadResult] containing the contents of the entry.
     */
    fun read(path: String) =
        archive.getInputSource(path)?.let { inputSource ->
            val xml = when {
                inputSource is LazyXMLInputSource -> inputSource.document
                isResXml(inputSource) -> module.loadResXmlDocument(
                    inputSource
                ).decodeToXml(resources.resourceTable.entryStore, resources.packageBlock?.id ?: 0)

                else -> null
            }

            ReadResult(
                xml != null,
                xml?.toText()?.toByteArray() ?: inputSource.openStream().use { it.readAllBytes() })
        }

    /**
     * Reads the manifest from the archive as an [AndroidManifestBlock].
     *
     * @return The [AndroidManifestBlock] contained in this archive.
     */
    fun readManifest(): AndroidManifestBlock =
        archive.getInputSource(AndroidManifestBlock.FILE_NAME).openStream().use { AndroidManifestBlock.load(it) }

    /**
     * Reads all dex files from the archive.
     *
     * @return A [Map] containing all the dex files.
     */
    fun readDexFiles() = module.listDexFiles().associate { file -> file.name to file.openStream().use { it.readAllBytes() } }

    /**
     * Write the byte array to the archive entry.
     *
     * @param path The archive path to read from.
     * @param content The content of the file.
     */
    fun writeRaw(path: String, content: ByteArray) =
        archive.add(ByteInputSource(content, path))

    /**
     * Write the XML to the entry associated.
     *
     * @param path The archive path to read from.
     * @param document The XML document to encode.
     */
    fun writeXml(path: String, document: XMLDocument) = archive.add(
        LazyXMLInputSource(
            path,
            document,
            resources,
        )
    )
}