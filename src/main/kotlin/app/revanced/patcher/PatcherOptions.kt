package app.revanced.patcher

import java.io.File

/**
 * Options for a patcher.
 * @param inputFile The input file (usually an apk file).
 * @param resourceCacheDirectory Directory to cache resources.
 * @param patchResources Weather to use the resource patcher. Resources will still need to be decoded.
 */
data class PatcherOptions(
    internal val inputFile: File,
    // TODO: maybe a file system in memory is better. Could cause high memory usage.
    internal val resourceCacheDirectory: String,
    internal val patchResources: Boolean = false
)
