package app.revanced.patcher

import app.revanced.patcher.patch.BytecodePatchContext
import app.revanced.patcher.patch.Patch
import app.revanced.patcher.patch.ResourcePatchContext
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
     * The set of [Patch]es.
     */
    internal val executablePatches = mutableSetOf<Patch<*>>()

    /**
     * The set of all [Patch]es and their dependencies.
     */
    internal val allPatches = mutableSetOf<Patch<*>>()

    /**
     * A context for patches containing the current state of the resources.
     */
    internal val resourcePatchContext = ResourcePatchContext(packageMetadata, config)

    /**
     * A context for patches containing the current state of the bytecode.
     */
    internal val bytecodePatchContext = BytecodePatchContext(config)
}
