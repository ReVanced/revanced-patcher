package app.revanced.patcher

import app.revanced.patcher.data.BytecodeContext
import app.revanced.patcher.data.ResourceContext
import app.revanced.patcher.patch.Patch
import brut.androlib.apk.ApkInfo
import brut.directory.ExtFile

/**
 * A context for the patcher containing the current state of the patcher.
 *
 * @param config The configuration for the patcher.
 */
@Suppress("MemberVisibilityCanBePrivate")
class PatcherContext internal constructor(config: PatcherConfig) {
    /**
     * [PackageMetadata] of the supplied [PatcherConfig.apkFile].
     */
    val packageMetadata = PackageMetadata(ApkInfo(ExtFile(config.apkFile)))

    /**
     * The map of [Patch]es associated by their [PatchClass].
     */
    internal val executablePatches = mutableMapOf<PatchClass, Patch<*>>()

    /**
     * The map of all [Patch]es and their dependencies associated by their [PatchClass].
     */
    internal val allPatches = mutableMapOf<PatchClass, Patch<*>>()

    /**
     * A context for the patcher containing the current state of the resources.
     */
    internal val resourceContext = ResourceContext(packageMetadata, config)

    /**
     * A context for the patcher containing the current state of the bytecode.
     */
    internal val bytecodeContext = BytecodeContext(config)
}
