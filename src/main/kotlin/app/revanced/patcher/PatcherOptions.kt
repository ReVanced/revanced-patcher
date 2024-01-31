package app.revanced.patcher

import app.revanced.patcher.data.ResourceContext
import brut.androlib.Config
import java.io.File
import java.util.logging.Logger

/**
 * Options for ReVanced [Patcher].
 * @param inputFile The input file to patch.
 * @param resourceCachePath The path to the directory to use for caching resources.
 * @param aaptBinaryPath The path to a custom aapt binary.
 * @param frameworkFileDirectory The path to the directory to cache the framework file in.
 * @param multithreadingDexFileWriter Whether to use multiple threads for writing dex files.
 * This can impact memory usage.
 */
data class PatcherOptions(
    internal val inputFile: File,
    internal val resourceCachePath: File = File("revanced-resource-cache"),
    internal val aaptBinaryPath: String? = null,
    internal val frameworkFileDirectory: String? = null,
    internal val multithreadingDexFileWriter: Boolean = false,
    internal val shortenResourcePaths: Boolean = false,
) {
    private val logger = Logger.getLogger(PatcherOptions::class.java.name)

    /**
     * The mode to use for resource decoding.
     * @see ResourceContext.ResourceDecodingMode
     */
    var resourceDecodingMode = ResourceContext.ResourceDecodingMode.MANIFEST_ONLY
        internal set

    /**
     * The configuration to use for resource decoding and compiling.
     */
    internal val resourceConfig =
        Config.getDefaultConfig().apply {
            useAapt2 = true
            aaptPath = aaptBinaryPath ?: ""
            frameworkDirectory = frameworkFileDirectory
            shortenResourcePaths = this@PatcherOptions.shortenResourcePaths
        }

    fun recreateResourceCacheDirectory() =
        resourceCachePath.also {
            if (it.exists()) {
                logger.info("Deleting existing resource cache directory")

                if (!it.deleteRecursively()) {
                    logger.severe("Failed to delete existing resource cache directory")
                }
            }

            it.mkdirs()
        }
}
