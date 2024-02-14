package app.revanced.patcher

import java.io.File

@Deprecated("Use PatcherConfig instead.")
data class PatcherOptions(
    internal val inputFile: File,
    internal val resourceCachePath: File = File("revanced-resource-cache"),
    internal val aaptBinaryPath: String? = null,
    internal val frameworkFileDirectory: String? = null,
    internal val multithreadingDexFileWriter: Boolean = false,
) {
    @Deprecated("This method will be removed in the future.")
    fun recreateResourceCacheDirectory(): File {
        PatcherConfig(
            inputFile,
            resourceCachePath,
            aaptBinaryPath,
            frameworkFileDirectory,
            multithreadingDexFileWriter,
        ).initializeTemporaryFilesDirectories()

        return resourceCachePath
    }
}
