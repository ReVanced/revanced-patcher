package app.revanced.arsc.resource

import app.revanced.arsc.ApkResourceException
import app.revanced.arsc.archive.Archive
import com.reandroid.apk.xmlencoder.EncodeUtil
import com.reandroid.arsc.chunk.PackageBlock
import com.reandroid.arsc.chunk.TableBlock
import com.reandroid.arsc.value.Entry
import com.reandroid.arsc.value.ResConfig
import java.io.File

/**
 * A high-level API for modifying the resources contained in an APK file.
 *
 * @param archive The [Archive] containing this resource table.
 * @param tableBlock The resources file of this APK file. Typically named "resources.arsc".
 */
class ResourceContainer(private val archive: Archive, internal val tableBlock: TableBlock?) {
    internal val packageBlock = tableBlock?.pickOne() // Pick the main PackageBlock.

    internal lateinit var resourceTable: ResourceTable

    init {
        archive.resources = this
    }

    /**
     * Open a resource file, creating it if the file does not exist.
     *
     * @param path The resource file path.
     * @return The corresponding [ResourceFile],
     */
    fun openFile(path: String) = ResourceFile(createHandle(path), archive)

    private fun getPackageBlock() = packageBlock ?: throw ApkResourceException.MissingResourceTable

    internal fun getOrCreateString(value: String) =
        tableBlock?.stringPool?.getOrCreate(value) ?: throw ApkResourceException.MissingResourceTable

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
        } catch (_: ApkResourceException.InvalidReference) {
            return null
        }

        val config = ResConfig.parse(qualifiers)
        return tableBlock?.resolveReference(resourceId)?.singleOrNull { it.resConfig == config }
    }

    /**
     * Create a [ResourceFile.Handle] that can be used to open a [ResourceFile].
     * This may involve looking it up in the resource table to find the actual location in the archive.
     *
     * @param path The path of the resource.
     */
    private fun createHandle(path: String): ResourceFile.Handle {
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
                onClose = { getOrCreateResource(type, name, StringResource(archivePath), qualifiers) }
            }
        }

        return ResourceFile.Handle(path, archivePath, onClose)
    }

    /**
     * Create or update a resource.
     *
     * @param type The resource type.
     * @param name The name of the resource.
     * @param resource The resource data.
     * @param qualifiers The resource configuration.
     * @return The resource ID for the resource.
     */
    fun getOrCreateResource(type: String, name: String, resource: Resource, qualifiers: String? = null) =
        getPackageBlock().getOrCreate(qualifiers, type, name).also { it.setTo(resource) }.resourceId

    /**
     * Create or update multiple resources in an ARSC type block.
     *
     * @param type The resource type.
     * @param map A map of resource names to the corresponding value.
     * @param configuration The resource configuration.
     */
    fun setGroup(type: String, map: Map<String, Resource>, configuration: String? = null) {
        getPackageBlock().getOrCreateSpecTypePair(type).getOrCreateTypeBlock(configuration).apply {
            map.forEach { (name, value) -> getOrCreateEntry(name).setTo(value) }
        }
    }

    /**
     * Update the [PackageBlock] name to match the manifest.
     */
    fun refreshPackageName() {
        packageBlock?.name = archive.readManifest().packageName
    }
}