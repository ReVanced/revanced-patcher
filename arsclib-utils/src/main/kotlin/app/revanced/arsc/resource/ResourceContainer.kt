package app.revanced.arsc.resource

import app.revanced.arsc.ApkException
import app.revanced.arsc.archive.Archive
import com.reandroid.apk.xmlencoder.EncodeUtil
import com.reandroid.arsc.chunk.TableBlock
import com.reandroid.arsc.value.Entry
import com.reandroid.arsc.value.ResConfig
import java.io.File

/**
 * A high-level API for modifying the resources contained in an Apk.
 *
 * @param tableBlock The resources.arsc file of this Apk.
 */
class ResourceContainer(private val archive: Archive, internal val tableBlock: TableBlock?) {
    internal val packageBlock = tableBlock?.pickOne() // Pick the main PackageBlock.

    internal lateinit var resourceTable: ResourceTable

    init {
        archive.resources = this
    }

    private fun expectPackageBlock() = packageBlock ?: throw ApkException.MissingResourceTable

    internal fun getOrCreateTableString(value: String) =
        tableBlock?.stringPool?.getOrCreate(value) ?: throw ApkException.MissingResourceTable

    /**
     * Set the value of the [Entry] to the one specified.
     *
     * @param value The new value.
     */
    private fun Entry.setTo(value: Resource) {
        val savedRef = specReference
        ensureComplex(value.complex)
        if (savedRef != 0) {
            // Preserve the entry name by restoring the previous spec block reference (if present).
            specReference = savedRef
        }

        value.write(this, this@ResourceContainer)
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
        } catch (_: ApkException.InvalidReference) {
            return null
        }

        val config = ResConfig.parse(qualifiers)
        return tableBlock?.resolveReference(resourceId)?.singleOrNull { it.resConfig == config }
    }

    /**
     * Create a [ResourceFile.Handle] that can be used to open a [ResourceFile].
     * This may involve looking it up in the resource table to find the actual location in the archive.
     *
     * @param resPath The path of the resource.
     */
    private fun createHandle(resPath: String): ResourceFile.Handle {
        if (resPath.startsWith("res/values")) throw ApkException.Decode("Decoding the resource table as a file is not supported")

        var callback = {}
        var archivePath = resPath

        if (tableBlock != null && resPath.startsWith("res/") && resPath.count { it == '/' } == 2) {
            val file = File(resPath)

            val qualifiers = EncodeUtil.getQualifiersFromResFile(file)
            val type = EncodeUtil.getTypeNameFromResFile(file)
            val name = file.nameWithoutExtension

            // The resource file names that the app developers used may have been minified, so we have to resolve it with the resource table.
            // Example: res/drawable-hdpi/icon.png -> res/4a.png
            val resolvedPath = getEntry(type, name, qualifiers)?.resValue?.valueAsString

            if (resolvedPath != null) {
                archivePath = resolvedPath
            } else {
                // An entry for this specific resource file was not found in the resource table, so we have to register it after we save.
                callback = { set(type, name, StringResource(archivePath), qualifiers) }
            }
        }

        return ResourceFile.Handle(resPath, archivePath, callback)
    }

    /**
     * Create or update an Android resource.
     *
     * @param type The resource type.
     * @param name The name of the resource.
     * @param value The resource data.
     * @param configuration The resource configuration.
     */
    fun set(type: String, name: String, value: Resource, configuration: String? = null) =
        expectPackageBlock().getOrCreate(configuration, type, name).also { it.setTo(value) }.resourceId

    /**
     * Create or update multiple resources in an ARSC type block.
     *
     * @param type The resource type.
     * @param map A map of resource names to the corresponding value.
     * @param configuration The resource configuration.
     */
    fun setGroup(type: String, map: Map<String, Resource>, configuration: String? = null) {
        expectPackageBlock().getOrCreateSpecTypePair(type).getOrCreateTypeBlock(configuration).apply {
            map.forEach { (name, value) -> getOrCreateEntry(name).setTo(value) }
        }
    }

    /**
     * Open a resource file, creating it if the file does not exist.
     *
     * @param path The resource file path.
     * @return The corresponding [ResourceFile],
     */
    fun openFile(path: String) = ResourceFile(
        createHandle(path), archive
    )

    /**
     * Update the [PackageBlock] name to match the manifest.
     */
    fun refreshPackageName() {
        packageBlock?.name = archive.readManifest().packageName
    }
}