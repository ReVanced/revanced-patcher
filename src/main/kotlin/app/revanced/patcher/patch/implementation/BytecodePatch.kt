package app.revanced.patcher.patch.implementation

import app.revanced.patcher.data.implementation.BytecodeData
import app.revanced.patcher.patch.base.Patch
import app.revanced.patcher.signature.implementation.method.MethodSignature

/**
 * Bytecode patch for the Patcher.
 * @param signatures A list of [MethodSignature] this patch relies on.
 */
abstract class BytecodePatch(
    val signatures: Iterable<MethodSignature>
) : Patch<BytecodeData>()