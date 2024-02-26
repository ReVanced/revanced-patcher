@file:Suppress("MemberVisibilityCanBePrivate")

package app.revanced.patcher.patch

import app.revanced.patcher.PatchClass
import app.revanced.patcher.Patcher
import app.revanced.patcher.PatcherContext
import app.revanced.patcher.data.Context
import app.revanced.patcher.extensions.AnnotationExtensions.findAnnotationRecursively
import app.revanced.patcher.patch.options.PatchOptions
import java.io.Closeable

/**
 * A patch.
 *
 * If an implementation of [Patch] also implements [Closeable]
 * it will be closed in reverse execution order of patches executed by [Patcher].
 *
 * @param T The [Context] type this patch will work on.
 */
sealed class Patch<out T : Context<*>> {
    /**
     * The name of the patch.
     */
    var name: String? = null
        private set

    /**
     * The description of the patch.
     */
    var description: String? = null
        private set

    /**
     * The packages the patch is compatible with.
     */
    var compatiblePackages: Set<CompatiblePackage>? = null
        private set

    /**
     * Other patches this patch depends on.
     */
    var dependencies: Set<PatchClass>? = null
        private set

    /**
     * Weather or not the patch should be used.
     */
    var use = true
        private set

    // TODO: Remove this property, once integrations are coupled with patches.
    /**
     * Weather or not the patch requires integrations.
     */
    var requiresIntegrations = false
        private set

    constructor(
        name: String?,
        description: String?,
        compatiblePackages: Set<CompatiblePackage>?,
        dependencies: Set<PatchClass>?,
        use: Boolean,
        requiresIntegrations: Boolean,
    ) {
        this.name = name
        this.description = description
        this.compatiblePackages = compatiblePackages
        this.dependencies = dependencies
        this.use = use
        this.requiresIntegrations = requiresIntegrations
    }

    constructor() {
        this::class.findAnnotationRecursively(app.revanced.patcher.patch.annotation.Patch::class)?.let { annotation ->
            this.name = annotation.name.ifEmpty { null }
            this.description = annotation.description.ifEmpty { null }
            this.compatiblePackages =
                annotation.compatiblePackages
                    .map { CompatiblePackage(it.name, it.versions.toSet().ifEmpty { null }) }
                    .toSet().ifEmpty { null }
            this.dependencies = annotation.dependencies.toSet().ifEmpty { null }
            this.use = annotation.use
            this.requiresIntegrations = annotation.requiresIntegrations
        }
    }

    /**
     * The options of the patch associated by the options key.
     */
    val options = PatchOptions()

    /**
     * The execution function of the patch.
     * This function is called by [Patcher].
     *
     * @param context The [PatcherContext] the patch will work on.
     */
    internal abstract fun execute(context: PatcherContext)

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
