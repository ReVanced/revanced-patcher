package app.revanced.patcher.patch

import app.revanced.patcher.BytecodeContext
import app.revanced.patcher.Context
import app.revanced.patcher.ResourceContext
import app.revanced.patcher.fingerprint.method.impl.MethodFingerprint
import java.io.Closeable

/**
 * A ReVanced patch.
 *
 * If it implements [Closeable], it will be closed after all patches have been executed.
 * Closing will be done in reverse execution order.
 */
sealed interface Patch<out T : Context> : Closeable {
    /**
     * The main function of the [Patch] which the patcher will call.
     *
     * @param context The [Context] the patch will work on.
     * @return The result of executing the patch.
     */
    fun execute(context: @UnsafeVariance T): PatchResult

    /**
     * The closing function for this patch.
     *
     * This can be treated like popping the patch from the current patch stack.
     */
    override fun close() {}
}

/**
 * Resource patch for the Patcher.
 */
interface ResourcePatch : Patch<ResourceContext>

/**
 * Bytecode patch for the Patcher.
 *
 * @param fingerprints A list of [MethodFingerprint] this patch relies on.
 */
abstract class BytecodePatch(
    internal val fingerprints: Iterable<MethodFingerprint>? = null
) : Patch<BytecodeContext>

/**
 * The class type of [Patch].
 */
typealias PatchClass = Class<out Patch<Context>>
