package app.revanced.patcher.patch.base

import app.revanced.patcher.data.base.Data
import app.revanced.patcher.patch.implementation.BytecodePatch
import app.revanced.patcher.patch.implementation.ResourcePatch
import app.revanced.patcher.patch.implementation.misc.PatchResult


/**
 * A ReVanced patch.
 * Can either be a [ResourcePatch] or a [BytecodePatch].
 */
abstract class Patch<out T : Data> {
    /**
     * The main function of the [Patch] which the patcher will call.
     */
    abstract fun execute(data: @UnsafeVariance T): PatchResult // FIXME: remove the UnsafeVariance annotation
}