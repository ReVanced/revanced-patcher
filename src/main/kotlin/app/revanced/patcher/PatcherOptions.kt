package app.revanced.patcher

import app.revanced.patcher.logging.Logger
import app.revanced.patcher.logging.impl.NopLogger
import java.io.File

/**
 * Options for the [Patcher].
 * @param inputFile The input file (usually an apk file).
 * @param resourceCacheDirectory Directory to cache resources.
 * @param patchResources Weather to use the resource patcher. Resources will still need to be decoded.
 * @param aaptPath Optional path to a custom aapt binary.
 * @param frameworkFolderLocation Optional path to a custom framework folder.
 * @param logger Custom logger implementation for the [Patcher].
 */
data class PatcherOptions(
    internal val inputFile: File,
    internal val resourceCacheDirectory: String,
    internal val patchResources: Boolean = false,
    internal val aaptPath: String = "",
    internal val frameworkFolderLocation: String? = null,
    internal val logger: Logger = NopLogger
)
