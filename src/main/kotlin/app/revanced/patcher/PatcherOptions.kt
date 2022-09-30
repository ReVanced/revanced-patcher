package app.revanced.patcher

import app.revanced.patcher.logging.Logger
import app.revanced.patcher.logging.impl.NopLogger

/**
 * Options for the [Patcher].
 * @param inputFiles The input files (usually apk files).
 * @param resourceCacheDirectory Directory to cache resources.
 * @param aaptPath Optional path to a custom aapt binary.
 * @param frameworkPath Optional path to a custom framework folder.
 * @param logger Custom logger implementation for the [Patcher].
 */
data class PatcherOptions(
    internal val inputFiles: List<Apk>,
    internal val resourceCacheDirectory: String,
    internal val aaptPath: String = "",
    internal val frameworkPath: String? = null,
    internal val logger: Logger = NopLogger
)
