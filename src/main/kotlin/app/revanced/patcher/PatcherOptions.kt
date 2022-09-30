package app.revanced.patcher

import app.revanced.patcher.apk.ApkBundle
import app.revanced.patcher.logging.Logger
import java.io.File

/**
 * Options for the [Patcher].
 * @param apkBundle The [ApkBundle].
 * @param workDirectory Directory to work in.
 * @param aaptPath Optional path to a custom aapt binary.
 * @param frameworkPath Optional path to a custom framework folder.
 * @param logger Custom logger implementation for the [Patcher].
 */
class PatcherOptions(
    internal val apkBundle: ApkBundle,
    workDirectory: String,
    internal val aaptPath: String? = null,
    internal val frameworkPath: String? = null,
    internal val logger: Logger = Logger.Nop
) {
    internal val workDirectory = File(workDirectory)
    internal val resourceDirectory = this.workDirectory.resolve(RESOURCE_PATH)
    internal val patchDirectory = this.workDirectory.resolve(PATCH_PATH)

    companion object {
        // Relative paths to PatcherOptions.workDirectory.
        private const val RESOURCE_PATH = "resources"
        private const val PATCH_PATH = "patch"
    }
}