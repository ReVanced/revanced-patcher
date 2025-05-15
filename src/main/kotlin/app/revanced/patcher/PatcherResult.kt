package app.revanced.patcher

import java.io.File
import java.io.InputStream

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

    /**
     * A dex file.
     *
     * @param name The original name of the dex file.
     * @param file The dex file as [File].
     */
    class PatchedDexFile internal constructor(val name: String, val file: File)

    /**
     * The resources of a patched apk.
     *
     * @param resourcesApk The compiled resources.apk file.
     * @param otherResources The directory containing other resources files.
     * @param doNotCompress List of files that should not be compressed.
     * @param deleteResources List of resources that should be deleted.
     */
    class PatchedResources internal constructor(
        val resourcesApk: File?,
        val otherResources: File?,
        val doNotCompress: Set<String>,
        val deleteResources: Set<String>,
    )
}
