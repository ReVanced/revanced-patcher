package app.revanced.patcher.patch.implementation

import app.revanced.patcher.data.implementation.ResourceData
import app.revanced.patcher.patch.base.Patch
import app.revanced.patcher.patch.implementation.metadata.PatchMetadata

/**
 * Resource patch for the Patcher.
 * @param metadata [PatchMetadata] for the patch.
 */
abstract class ResourcePatch(
    override val metadata: PatchMetadata
) : Patch<ResourceData>(metadata)