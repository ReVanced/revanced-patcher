package app.revanced.patcher.patch

import app.revanced.patcher.data.Data
import app.revanced.patcher.patch.impl.BytecodePatch
import app.revanced.patcher.patch.impl.ResourcePatch
import java.io.Closeable

/**
 * A ReVanced patch.
 *
 * Can either be a [ResourcePatch] or a [BytecodePatch].
 * If it implements [Closeable], it will be closed after all patches have been executed.
 * Closing will be done in reverse execution order.
 */
abstract class Patch<out T : Data> {
    /**
     * The main function of the [Patch] which the patcher will call.
     */
    abstract fun execute(data: @UnsafeVariance T): PatchResult
}

abstract class OptionsContainer {
    /**
     * A list of [PatchOption]s.
     * @see PatchOptions
     */
    @Suppress("MemberVisibilityCanBePrivate")
    val options = PatchOptions()

    protected fun <T> option(opt: PatchOption<T>): PatchOption<T> {
        options.register(opt)
        return opt
    }
}