package app.revanced.arsc.resource

import app.revanced.arsc.ApkResourceException
import com.reandroid.apk.xmlencoder.EncodeException
import com.reandroid.apk.xmlencoder.EncodeMaterials
import com.reandroid.arsc.util.FrameworkTable
import com.reandroid.arsc.value.Entry
import com.reandroid.common.TableEntryStore

/**
 * A high-level API for resolving resources in the resource table, which spans the entire ApkBundle.
 */
class ResourceTable(base: ResourceContainer, all: Sequence<ResourceContainer>) {
    private val packageName = base.packageBlock!!.name

    /**
     * A [TableEntryStore] used to decode XML.
     */
    internal val entryStore = TableEntryStore()

    /**
     * The [EncodeMaterials] to use for resolving resources and encoding XML.
     */
    internal val encodeMaterials: EncodeMaterials = object : EncodeMaterials() {
        /*
        Our implementation is more efficient because it does not have to loop through every single entry group
        when the resource id cannot be found in the TableIdentifier, which does not update when you create a new resource.
        It also looks at the entire table instead of just the current package.
        */
        override fun resolveLocalResourceId(type: String, name: String) = resolveLocal(type, name)
    }

    /**
     * The resource mappings which are generated when the ApkBundle is created.
     */
    private val tableIdentifier = encodeMaterials.tableIdentifier

    /**
     * A table of all the resources that have been changed or added.
     */
    private val modifiedResources = HashMap<String, HashMap<String, Int>>()


    /**
     * Resolve a resource id for the specified resource.
     * Cannot resolve resources from the android framework.
     *
     * @param type The type of the resource.
     * @param name The name of the resource.
     * @return The id of the resource.
     */
    fun resolveLocal(type: String, name: String) =
        modifiedResources[type]?.get(name)
            ?: tableIdentifier.get(packageName, type, name)?.resourceId
            ?: throw ApkResourceException.InvalidReference(
                type,
                name
            )

    /**
     * Resolve a resource id for the specified resource.
     *
     * @param reference The resource reference string.
     * @return The id of the resource.
     */
    fun resolve(reference: String) = try {
        encodeMaterials.resolveReference(reference)
    } catch (e: EncodeException) {
        throw ApkResourceException.InvalidReference(reference, e)
    }

    /**
     * Notify the [ResourceTable] that an [Entry] has been created or modified.
     */
    internal fun registerChanged(entry: Entry) {
        modifiedResources.getOrPut(entry.typeName, ::HashMap)[entry.name] = entry.resourceId
    }

    init {
        all.forEach {
            it.tableBlock?.let { table ->
                entryStore.add(table)
                tableIdentifier.load(table)
            }

            it.resourceTable = this
        }

        base.also {
            encodeMaterials.currentPackage = it.packageBlock

            it.tableBlock!!.frameWorks.forEach { fw ->
                if (fw is FrameworkTable) {
                    entryStore.add(fw)
                    encodeMaterials.addFramework(fw)
                }
            }
        }
    }
}