package app.revanced.patcher.patch

import app.revanced.patcher.data.BytecodeContext
import app.revanced.patcher.fingerprint.MethodFingerprint

/**
 * A ReVanced [Patch] that works on [BytecodeContext].
 *
 * @param fingerprints A list of [MethodFingerprint]s which will be resolved before the patch is executed.
 */
abstract class BytecodePatch(
    internal val fingerprints : Set<MethodFingerprint> = emptySet(),
) : Patch<BytecodeContext>()