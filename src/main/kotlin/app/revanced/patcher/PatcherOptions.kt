package app.revanced.patcher

import app.revanced.patcher.data.ResourceContext
import app.revanced.patcher.logging.impl.NopLogger
import brut.androlib.Config
import java.io.File
import java.util.logging.Logger

/**
 * Options for ReVanced [Patcher].
 * @param inputFile The input file to patch.
 * @param resourceCachePath The path to the directory to use for caching resources.
 * @param aaptBinaryPath The path to a custom aapt binary.
 * @param frameworkFileDirectory The path to the directory to cache the framework file in.
 * @param unusedLogger The logger to use for logging.
 */
data class PatcherOptions
@Deprecated("Use the constructor without the logger parameter instead")
constructor(
    internal val inputFile: File,
    internal val resourceCachePath: File = File("revanced-resource-cache"),
    internal val aaptBinaryPath: String? = null,
    internal val frameworkFileDirectory: String? = null,
    internal val unusedLogger: app.revanced.patcher.logging.Logger = NopLogger
) {
    private val logger = Logger.getLogger(PatcherOptions::class.java.name)

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

    /**
     * Options for ReVanced [Patcher].
     * @param inputFile The input file to patch.
     * @param resourceCachePath The path to the directory to use for caching resources.
     * @param aaptBinaryPath The path to a custom aapt binary.
     * @param frameworkFileDirectory The path to the directory to cache the framework file in.
     */
    constructor(
        inputFile: File,
        resourceCachePath: File = File("revanced-resource-cache"),
        aaptBinaryPath: String? = null,
        frameworkFileDirectory: String? = null,
    ) : this(
        inputFile,
        resourceCachePath,
        aaptBinaryPath,
        frameworkFileDirectory,
        NopLogger
    )

    fun recreateResourceCacheDirectory() = resourceCachePath.also {
        if (it.exists()) {
            logger.info("Deleting existing resource cache directory")

            if (!it.deleteRecursively())
                logger.severe("Failed to delete existing resource cache directory")
        }

        it.mkdirs()
    }
}
