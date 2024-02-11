package app.revanced.patcher

import java.io.File
import java.io.InputStream
import kotlin.jvm.internal.Intrinsics

/**
 * The result of a patcher.
 *
 * @param dexFiles The patched dex files.
 * @param resources The patched resources.
 */
@Suppress("MemberVisibilityCanBePrivate")
class PatcherResult internal constructor(
    val dexFiles: Set<PatchedDexFile>,
    val resources: PatchedResources?,
) {
    @Deprecated("This method is not used anymore")
    constructor(
        dexFiles: List<PatchedDexFile>,
        resourceFile: File?,
        doNotCompress: List<String>? = null,
    ) : this(dexFiles.toSet(), PatchedResources(resourceFile, null, doNotCompress?.toSet() ?: emptySet(), emptySet()))

    @Deprecated("This method is not used anymore")
    fun component1(): List<PatchedDexFile> {
        return dexFiles.toList()
    }

    @Deprecated("This method is not used anymore")
    fun component2(): File? {
        return resources?.resourcesApk
    }

    @Deprecated("This method is not used anymore")
    fun component3(): List<String>? {
        return resources?.doNotCompress?.toList()
    }

    @Deprecated("This method is not used anymore")
    fun copy(
        dexFiles: List<PatchedDexFile>,
        resourceFile: File?,
        doNotCompress: List<String>? = null,
    ): PatcherResult {
        return PatcherResult(
            dexFiles.toSet(),
            PatchedResources(
                resourceFile,
                null,
                doNotCompress?.toSet() ?: emptySet(),
                emptySet(),
            ),
        )
    }

    @Deprecated("This method is not used anymore")
    override fun toString(): String {
        return (("PatcherResult(dexFiles=" + this.dexFiles + ", resourceFile=" + this.resources?.resourcesApk) + ", doNotCompress=" + this.resources?.doNotCompress) + ")"
    }

    @Deprecated("This method is not used anymore")
    override fun hashCode(): Int {
        val result = dexFiles.hashCode()
        return (
            (
                (result * 31) +
                    (if (this.resources?.resourcesApk == null) 0 else this.resources?.resourcesApk.hashCode())
                ) * 31
            ) +
            (if (this.resources?.doNotCompress == null) 0 else this.resources?.doNotCompress.hashCode())
    }

    @Deprecated("This method is not used anymore")
    override fun equals(other: Any?): Boolean {
        if (this === other) {
            return true
        }
        if (other is PatcherResult) {
            return Intrinsics.areEqual(this.dexFiles, other.dexFiles) && Intrinsics.areEqual(
                this.resources?.resourcesApk,
                other.resources?.resourcesApk,
            ) && Intrinsics.areEqual(this.resources?.doNotCompress, other.resources?.doNotCompress)
        }
        return false
    }

    @Deprecated("This method is not used anymore")
    fun getDexFiles() = component1()

    @Deprecated("This method is not used anymore")
    fun getResourceFile() = component2()

    @Deprecated("This method is not used anymore")
    fun getDoNotCompress() = component3()

    /**
     * A dex file.
     *
     * @param name The original name of the dex file.
     * @param stream The dex file as [InputStream].
     */
    class PatchedDexFile
    // TODO: Add internal modifier.
    @Deprecated("This constructor will be removed in the future.")
    constructor(val name: String, val stream: InputStream)

    /**
     * The resources of a patched apk.
     *
     * @param resourcesApk The compiled resources.apk file.
     * @param otherResources The directory containing other resources files.
     * @param doNotCompress List of files that should not be compressed.
     * @param deleteResources List of predicates about resources that should be deleted.
     */
    class PatchedResources internal constructor(
        val resourcesApk: File?,
        val otherResources: File?,
        val doNotCompress: Set<String>,
        val deleteResources: Set<(String) -> Boolean>,
    )
}
