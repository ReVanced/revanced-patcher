package app.revanced.patcher.patch

import app.revanced.patcher.PatchClass
import app.revanced.patcher.data.ResourceContext

/**
 * A ReVanced [Patch] that works on [ResourceContext].
 *
 * @param name The name of the patch.
 * @param description The description of the patch.
 * @param compatiblePackages The packages the patch is compatible with.
 * @param dependencies The names of patches this patch depends on.
 * @param use Weather or not the patch should be used.
 * @param requiresIntegrations Weather or not the patch requires integrations.
 */
abstract class ResourcePatch(
    name: String? = null,
    description: String? = null,
    compatiblePackages: Set<CompatiblePackage>? = null,
    dependencies: Set<PatchClass>? = null,
    use: Boolean = true,
    // TODO: Remove this property, once integrations are coupled with patches.
    requiresIntegrations: Boolean = false,
) : Patch<ResourceContext>(name, description, compatiblePackages, dependencies, use, requiresIntegrations)