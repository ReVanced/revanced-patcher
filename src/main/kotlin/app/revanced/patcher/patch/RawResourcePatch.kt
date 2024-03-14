package app.revanced.patcher.patch

import app.revanced.patcher.PatchClass
import app.revanced.patcher.Patcher
import app.revanced.patcher.PatcherContext
import app.revanced.patcher.data.ResourceContext
import java.io.Closeable

/**
 * A [Patch] that accesses a [ResourceContext].
 *
 * If an implementation of [Patch] also implements [Closeable]
 * it will be closed in reverse execution order of patches executed by [Patcher].
 *
 * This type of patch that does not have access to decoded resources.
 * Instead, you can read and write arbitrary files in an APK file.
 *
 * If you want to access decoded resources, use [ResourcePatch] instead.
 */
abstract class RawResourcePatch : Patch<ResourceContext> {
    /**
     * Create a new [RawResourcePatch].
     */
    constructor()

    /**
     * Create a new [RawResourcePatch].
     *
     * @param name The name of the patch.
     * @param description The description of the patch.
     * @param compatiblePackages The packages the patch is compatible with.
     * @param dependencies Other patches this patch depends on.
     * @param use Weather or not the patch should be used.
     * @param requiresIntegrations Weather or not the patch requires integrations.
     */
    constructor(
        name: String? = null,
        description: String? = null,
        compatiblePackages: Set<CompatiblePackage>? = null,
        dependencies: Set<PatchClass>? = null,
        use: Boolean = true,
        requiresIntegrations: Boolean = false,
    ) : super(name, description, compatiblePackages, dependencies, use, requiresIntegrations)

    override fun execute(context: PatcherContext) = execute(context.resourceContext)
}
