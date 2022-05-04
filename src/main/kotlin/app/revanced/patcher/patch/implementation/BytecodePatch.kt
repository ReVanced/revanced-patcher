package app.revanced.patcher.patch.implementation

import app.revanced.patcher.data.implementation.BytecodeData
import app.revanced.patcher.patch.base.Patch
import app.revanced.patcher.patch.implementation.metadata.PatchMetadata
import app.revanced.patcher.signature.MethodSignature

/**
 * Bytecode patch for the Patcher.
 * @param metadata [PatchMetadata] for the patch.
 * @param signatures A list of [MethodSignature] this patch relies on.
 */
abstract class BytecodePatch(
    override val metadata: PatchMetadata,
    val signatures: Iterable<MethodSignature>
) : Patch<BytecodeData>(metadata)