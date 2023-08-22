package app.revanced.patcher

import app.revanced.patcher.data.ResourceContext
import app.revanced.patcher.logging.Logger
import app.revanced.patcher.logging.impl.NopLogger
import brut.androlib.Config
import java.io.File

/**
 * Options for ReVanced [Patcher].
 * @param inputFile The input file to patch.
 * @param resourceCachePath The path to the directory to use for caching resources.
 * @param aaptBinaryPath The path to a custom aapt binary.
 * @param frameworkFileDirectory The path to the directory to cache the framework file in.
 * @param logger A [Logger].
 */
data class PatcherOptions(
    internal val inputFile: File,
    internal val resourceCachePath: File = File("revanced-resource-cache"),
    internal val aaptBinaryPath: String? = null,
    internal val frameworkFileDirectory: String? = null,
    internal val logger: Logger = NopLogger
) {
    /**
     * The mode to use for resource decoding.
     * @see ResourceContext.ResourceDecodingMode
     */
    internal var resourceDecodingMode = ResourceContext.ResourceDecodingMode.MANIFEST_ONLY

    /**
     * The configuration to use for resource decoding and compiling.
     */
    internal val resourceConfig = Config.getDefaultConfig().apply {
        useAapt2 = true
        aaptPath = aaptBinaryPath ?: ""
        frameworkDirectory = frameworkFileDirectory
    }

    fun recreateResourceCacheDirectory() = resourceCachePath.also {
        if (it.exists()) {
            logger.info("Deleting existing resource cache directory")

            if (!it.deleteRecursively())
                logger.error("Failed to delete existing resource cache directory")
        }

        it.mkdirs()
    }
}
