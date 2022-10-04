package app.revanced.patcher.patch

import app.revanced.patcher.data.Data
import app.revanced.patcher.data.impl.BytecodeData
import app.revanced.patcher.data.impl.ResourceData
import app.revanced.patcher.fingerprint.method.impl.MethodFingerprint
import java.io.Closeable

/**
 * A ReVanced patch.
 *
 * Can either be a [ResourcePatch] or a [BytecodePatch].
 * If it implements [Closeable], it will be closed after all patches have been executed.
 * Closing will be done in reverse execution order.
 */
sealed interface Patch<out T : Data> {
    /**
     * The main function of the [Patch] which the patcher will call.
     */
    fun execute(data: @UnsafeVariance T): PatchResult
}

/**
 * Resource patch for the Patcher.
 */
interface ResourcePatch : Patch<ResourceData>

/**
 * Bytecode patch for the Patcher.
 * @param fingerprints A list of [MethodFingerprint] this patch relies on.
 */
abstract class BytecodePatch(
    internal val fingerprints: Iterable<MethodFingerprint>? = null
) : Patch<BytecodeData>