package app.revanced.patcher

import java.io.File
import java.io.InputStream

/**
 * The result of a patcher.
 * @param dexFiles The patched dex files.
 * @param resourceFile File containing resources that need to be extracted into the APK.
 * @param doNotCompress List of relative paths of files to exclude from compressing.
 */
data class PatcherResult(
    val dexFiles: List<PatchedDexFile>,
    val resourceFile: File?,
    val doNotCompress: List<String>? = null
) {
    /**
     * Wrapper for dex files.
     * @param name The original name of the dex file.
     * @param stream The dex file as [InputStream].
     */
    class PatchedDexFile(val name: String, val stream: InputStream)
}