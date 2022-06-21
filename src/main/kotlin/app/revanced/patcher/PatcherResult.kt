package app.revanced.patcher

import app.revanced.patcher.util.dex.DexFile
import brut.directory.ExtFile

/**
 * The result of a patcher.
 * @param dexFiles The patched dex files.
 * @param doNotCompress List of relative paths to files to exclude from compressing.
 * @param resourceFile ExtFile containing resources that need to be extracted into the APK.
 */
data class PatcherResult(
    val dexFiles: List<DexFile>,
    val doNotCompress: List<String>? = null,
    val resourceFile: ExtFile?
)