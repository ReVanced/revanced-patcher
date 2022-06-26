package app.revanced.patcher.patch

import app.revanced.patcher.data.Data
import app.revanced.patcher.patch.impl.BytecodePatch
import app.revanced.patcher.patch.impl.ResourcePatch


/**
 * A ReVanced patch.
 * Can either be a [ResourcePatch] or a [BytecodePatch].
 */
abstract class Patch<out T : Data> {

    /**
     * The main function of the [Patch] which the patcher will call.
     */
    abstract fun execute(data: @UnsafeVariance T): PatchResult
}