package app.revanced.patcher.patch

import app.revanced.patcher.PatchClass
import app.revanced.patcher.Patcher
import app.revanced.patcher.data.BytecodeContext
import app.revanced.patcher.data.Context
import app.revanced.patcher.data.ResourceContext
import app.revanced.patcher.fingerprint.method.impl.MethodFingerprint
import java.io.Closeable

/**
 * A ReVanced patch.
 *
 * If an implementation of [Patch] also implements [Closeable]
 * it will be closed in reverse execution order of patches executed by ReVanced [Patcher].
 *
 * @param manifest The manifest of the [Patch].
 * @param T The [Context] type this patch will work on.
 */
sealed class Patch<out T : Context<*>>(val manifest: Manifest) {
    /**
     * The execution function of the patch.
     *
     * @param context The [Context] the patch will work on.
     * @return The result of executing the patch.
     */
    abstract fun execute(context: @UnsafeVariance T)

    override fun hashCode() = manifest.hashCode()

    override fun equals(other: Any?) = other is Patch<*> && manifest == other.manifest

    override fun toString() = manifest.name

    /**
     * The manifest of a [Patch].
     *
     * @param name The name of the patch.
     * @param description The description of the patch.
     * @param use Weather or not the patch should be used.
     * @param dependencies The names of patches this patch depends on.
     * @param compatiblePackages The packages the patch is compatible with.
     * @param requiresIntegrations Weather or not the patch requires integrations.
     * @param options The options of the patch.
     */
    class Manifest(
        val name: String,
        val description: String,
        val use: Boolean = true,
        val dependencies: Set<PatchClass>? = null,
        val compatiblePackages: Set<CompatiblePackage>? = null,
        // TODO: Remove this property, once integrations are coupled with patches.
        val requiresIntegrations: Boolean = false,
        val options: PatchOptions? = null,
    ) {
        override fun hashCode() = name.hashCode()
        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (javaClass != other?.javaClass) return false

            other as Manifest

            return name == other.name
        }

        /**
         * A package a [Patch] is compatible with.
         *
         * @param name The name of the package.
         * @param versions The versions of the package.
         */
        class CompatiblePackage(
            val name: String,
            val versions: Set<String>? = null,
        )
    }
}

/**
 * A ReVanced [Patch] that works on [ResourceContext].
 *
 * @param metadata The manifest of the [ResourcePatch].
 */
abstract class ResourcePatch(
    metadata: Manifest,
) : Patch<ResourceContext>(metadata)

/**
 * A ReVanced [Patch] that works on [BytecodeContext].
 *
 * @param manifest The manifest of the [BytecodePatch].
 * @param fingerprints A list of [MethodFingerprint]s which will be resolved before the patch is executed.
 */
abstract class BytecodePatch(
    manifest: Manifest,
    internal vararg val fingerprints: MethodFingerprint,
) : Patch<BytecodeContext>(manifest)