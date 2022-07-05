package app.revanced.patcher.patch.impl

import app.revanced.patcher.data.impl.BytecodeData
import app.revanced.patcher.fingerprint.method.impl.MethodFingerprint
import app.revanced.patcher.patch.Patch

/**
 * Bytecode patch for the Patcher.
 * @param fingerprints A list of [MethodFingerprint] this patch relies on.
 */
abstract class BytecodePatch(
    internal val fingerprints: Iterable<MethodFingerprint>? = null
) : Patch<BytecodeData>()
