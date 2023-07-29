package app.revanced.arsc.resource

import app.revanced.arsc.ApkResourceException
import app.revanced.arsc.archive.Archive
import com.reandroid.apk.xmlencoder.EncodeUtil
import com.reandroid.arsc.chunk.TableBlock
import com.reandroid.arsc.chunk.xml.ResXmlDocument
import com.reandroid.arsc.value.Entry
import com.reandroid.arsc.value.ResConfig
import java.io.Closeable
import java.io.File
import java.io.Flushable

class ResourceContainer(private val archive: Archive, internal val tableBlock: TableBlock) : Flushable {
    private val packageBlock = tableBlock.pickOne() // Pick the main package block.
    internal lateinit var resourceTable: ResourceTable // TODO: Set this.

    private val lockedResourceFileNames = mutableSetOf<String>()

    private fun lock(resourceFile: ResourceFile) {
        if (resourceFile.name in lockedResourceFileNames) {
            throw ApkResourceException.Locked("Resource file ${resourceFile.name} is already locked.")
        }

        lockedResourceFileNames.add(resourceFile.name)
    }

    private fun unlock(resourceFile: ResourceFile) {
        lockedResourceFileNames.remove(resourceFile.name)
    }


    fun <T : ResourceFile> openResource(name: String): ResourceFileEditor<T> {
        val inputSource = archive.read(name)
            ?: throw ApkResourceException.Read("Resource file $name not found.")

        val resourceFile = when {
            ResXmlDocument.isResXmlBlock(inputSource.openStream()) -> {
                val xmlDocument = archive.module
                    .loadResXmlDocument(inputSource)
                    .decodeToXml(resourceTable.entryStore, packageBlock.id)

                ResourceFile.XmlResourceFile(name, xmlDocument)
            }

            else -> {
                val bytes = inputSource.openStream().use { it.readAllBytes() }

                ResourceFile.BinaryResourceFile(name, bytes)
            }
        }

        try {
            @Suppress("UNCHECKED_CAST")
            return ResourceFileEditor(resourceFile as T).also {
                lockedResourceFileNames.add(name)
            }
        } catch (e: ClassCastException) {
            throw ApkResourceException.Decode("Resource file $name is not ${resourceFile::class}.", e)
        }
    }

    inner class ResourceFileEditor<T : ResourceFile> internal constructor(
        private val resourceFile: T,
    ) : Closeable {
        fun use(block: (T) -> Unit) = block(resourceFile)
        override fun close() {
            lockedResourceFileNames.remove(resourceFile.name)
        }
    }

    override fun flush() {
        TODO("Not yet implemented")
    }

    /**
     * Open a resource file, creating it if the file does not exist.
     *
     * @param path The resource file path.
     * @return The corresponding [ResourceFiles],
     */
    fun openFile(path: String) = ResourceFiles(createHandle(path), archive)

    private fun getPackageBlock() = packageBlock ?: throw ApkResourceException.MissingResourceTable

    internal fun getOrCreateString(value: String) =
        tableBlock?.stringPool?.getOrCreate(value) ?: throw ApkResourceException.MissingResourceTable

    private fun Entry.set(resource: Resource) {
        val existingEntryNameReference = specReference

        // Sets this.specReference if the entry is not yet initialized.
        // Sets this.specReference to 0 if the resource type of the existing entry changes.
        ensureComplex(resource.isComplex)

        if (existingEntryNameReference != 0) {
            // Preserve the entry name by restoring the previous spec block reference (if present).
            specReference = existingEntryNameReference
        }

        resource.write(this, this@ResourceContainer)
        resourceTable.registerChanged(this)
    }

    /**
     * Retrieve an [Entry] from the resource table.
     *
     * @param type The resource type.
     * @param name The resource name.
     * @param qualifiers The variant to use.
     */
    private fun getEntry(type: String, name: String, qualifiers: String?): Entry? {
        val resourceId = try {
            resourceTable.resolve("@$type/$name")
        } catch (_: ApkResourceException.InvalidReference) {
            return null
        }

        val config = ResConfig.parse(qualifiers)
        return tableBlock?.resolveReference(resourceId)?.singleOrNull { it.resConfig == config }
    }

    /**
     * Create a [ResourceFiles.Handle] that can be used to open a [ResourceFiles].
     * This may involve looking it up in the resource table to find the actual location in the archive.
     *
     * @param path The path of the resource.
     */
    private fun createHandle(path: String): ResourceFiles.Handle {
        if (path.startsWith("res/values")) throw ApkResourceException.Decode("Decoding the resource table as a file is not supported")

        var onClose = {}
        var archivePath = path

        if (tableBlock != null && path.startsWith("res/") && path.count { it == '/' } == 2) {
            val file = File(path)

            val qualifiers = EncodeUtil.getQualifiersFromResFile(file)
            val type = EncodeUtil.getTypeNameFromResFile(file)
            val name = file.nameWithoutExtension

            // The resource file names that the app developers used may have been minified, so we have to resolve it with the resource table.
            // Example: res/drawable-hdpi/icon.png -> res/4a.png
            getEntry(type, name, qualifiers)?.resValue?.valueAsString?.let {
                archivePath = it
            } ?: run {
                // An entry for this specific resource file was not found in the resource table, so we have to register it after we save.
                onClose = { setResource(type, name, StringResource(archivePath), qualifiers) }
            }
        }

        return ResourceFiles.Handle(path, archivePath, onClose)
    }

    fun setResource(type: String, entryName: String, resource: Resource, qualifiers: String? = null) =
        getPackageBlock().getOrCreate(qualifiers, type, entryName).also { it.set(resource) }.resourceId

    fun setResources(type: String, resources: Map<String, Resource>, configuration: String? = null) {
        getPackageBlock().getOrCreateSpecTypePair(type).getOrCreateTypeBlock(configuration).apply {
            resources.forEach { (entryName, resource) -> getOrCreateEntry(entryName).set(resource) }
        }
    }

    override fun flush() {
        packageBlock?.name = archive
    }
}