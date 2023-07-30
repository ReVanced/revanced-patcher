package app.revanced.patcher

import app.revanced.patcher.logging.Logger
import app.revanced.patcher.logging.impl.NopLogger
import java.io.File

/**
 * Options for the [Patcher].
 * @param inputFile The input file (usually an apk file).
 * @param resourceCacheDirectory Directory to cache resources.
 * @param aaptPath Optional path to a custom aapt binary.
 * @param frameworkDirectory Optional path to a custom framework directory.
 * @param logger Custom logger implementation for the [Patcher].
 */
data class PatcherOptions(
    internal val inputFile: File,
    internal val resourceCacheDirectory: String,
    internal val aaptPath: String? = null,
    internal val frameworkDirectory: String? = null,
    internal val logger: Logger = NopLogger
)
