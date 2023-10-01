@file:Suppress("MemberVisibilityCanBePrivate")

package app.revanced.patcher.patch

import app.revanced.patcher.PatchClass
import app.revanced.patcher.Patcher
import app.revanced.patcher.data.Context
import app.revanced.patcher.patch.options.PatchOptions
import java.io.Closeable

/**
 * A ReVanced patch.
 *
 * If an implementation of [Patch] also implements [Closeable]
 * it will be closed in reverse execution order of patches executed by ReVanced [Patcher].
 *
 * @param name The name of the patch.
 * @param description The description of the patch.
 * @param compatiblePackages The packages the patch is compatible with.
 * @param dependencies The names of patches this patch depends on.
 * @param use Weather or not the patch should be used.
 * @param requiresIntegrations Weather or not the patch requires integrations.
 * @param T The [Context] type this patch will work on.
 */
sealed class Patch<out T : Context<*>>(
    val name: String? = null,
    val description: String? = null,
    val compatiblePackages: Set<CompatiblePackage>? = null,
    val dependencies: Set<PatchClass>? = null,
    val use: Boolean = true,
    // TODO: Remove this property, once integrations are coupled with patches.
    val requiresIntegrations: Boolean = false,
) {
    /**
     * The options of the patch associated by the options key.
     */
    val options = PatchOptions()

    /**
     * The execution function of the patch.
     *
     * @param context The [Context] the patch will work on.
     * @return The result of executing the patch.
     */
    abstract fun execute(context: @UnsafeVariance T)

    override fun hashCode() = name.hashCode()

    override fun toString() = name ?: this::class.simpleName ?: "Unnamed patch"

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as Patch<*>

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

