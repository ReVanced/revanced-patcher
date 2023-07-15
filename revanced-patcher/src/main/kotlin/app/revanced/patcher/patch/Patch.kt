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
sealed interface Patch<out T : Context> {
    /**
     * The main function of the [Patch] which the patcher will call.
     *
     * @param context The [Context] the patch will work on.
     */
    suspend fun execute(context: @UnsafeVariance T)
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

// TODO: populate this everywhere where the alias is not used yet
/**
 * The class type of [Patch].
 */
typealias PatchClass = Class<out Patch<Context>>
