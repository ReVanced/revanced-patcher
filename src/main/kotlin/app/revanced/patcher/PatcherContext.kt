package app.revanced.patcher

import app.revanced.patcher.data.BytecodeContext
import app.revanced.patcher.data.ResourceContext
import app.revanced.patcher.patch.Patch
import app.revanced.patcher.patch.PatchClass
import brut.androlib.apk.ApkInfo
import brut.directory.ExtFile

/**
 * A context for ReVanced [Patcher].
 *
 * @param options The [PatcherOptions] used to create this context.
 */
class PatcherContext internal constructor(options: PatcherOptions) {
    /**
     * [PackageMetadata] of the supplied [PatcherOptions.inputFile].
     */
    val packageMetadata = PackageMetadata(ApkInfo(ExtFile(options.inputFile)))

    /**
     * The list of [Patch]es to execute.
     */
    internal val patches = mutableListOf<PatchClass>()

    /**
     * The [ResourceContext] of this [PatcherContext].
     * This holds the current state of the resources.
     */
    internal val resourceContext = ResourceContext(this, options)

    /**
     * The [BytecodeContext] of this [PatcherContext].
     * This holds the current state of the bytecode.
     */
    internal val bytecodeContext = BytecodeContext(options)
}