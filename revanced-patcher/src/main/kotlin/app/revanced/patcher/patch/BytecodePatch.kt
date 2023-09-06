package app.revanced.patcher.patch

import app.revanced.patcher.PatchClass
import app.revanced.patcher.data.BytecodeContext
import app.revanced.patcher.fingerprint.method.impl.MethodFingerprint

/**
 * A ReVanced [Patch] that works on [BytecodeContext].
 *
 * @param fingerprints A list of [MethodFingerprint]s which will be resolved before the patch is executed.
 * @param name The name of the patch.
 * @param description The description of the patch.
 * @param compatiblePackages The packages the patch is compatible with.
 * @param dependencies The names of patches this patch depends on.
 * @param use Weather or not the patch should be used.
 * @param requiresIntegrations Weather or not the patch requires integrations.
 */
abstract class BytecodePatch(
    internal val fingerprints: Set<MethodFingerprint> = emptySet(),
    name: String? = null,
    description: String? = null,
    compatiblePackages: Set<CompatiblePackage>? = null,
    dependencies: Set<PatchClass>? = null,
    use: Boolean = true,
    // TODO: Remove this property, once integrations are coupled with patches.
    requiresIntegrations: Boolean = false,
) : Patch<BytecodeContext>(name, description, compatiblePackages, dependencies, use, requiresIntegrations)