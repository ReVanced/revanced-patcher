package app.revanced.patcher.extensions

import app.revanced.patcher.annotation.Compatibility
import app.revanced.patcher.annotation.Description
import app.revanced.patcher.annotation.Name
import app.revanced.patcher.annotation.Version
import app.revanced.patcher.data.Context
import app.revanced.patcher.extensions.AnnotationExtensions.findAnnotationRecursively
import app.revanced.patcher.patch.OptionsContainer
import app.revanced.patcher.patch.Patch
import app.revanced.patcher.patch.PatchOptions
import app.revanced.patcher.patch.annotations.DependsOn
import app.revanced.patcher.patch.annotations.RequiresIntegrations
import kotlin.reflect.KVisibility
import kotlin.reflect.full.companionObject
import kotlin.reflect.full.companionObjectInstance

object PatchExtensions {
    /**
     * The name of a [Patch].
     */
    val Class<out Patch<Context>>.patchName: String
        get() = findAnnotationRecursively(Name::class)?.name ?: this.simpleName

    /**
     * The version of a [Patch].
     */
    val Class<out Patch<Context>>.version
        get() = findAnnotationRecursively(Version::class)?.version

    /**
     * Weather or not a [Patch] should be included.
     */
    val Class<out Patch<Context>>.include
        get() = findAnnotationRecursively(app.revanced.patcher.patch.annotations.Patch::class)!!.include

    /**
     * The description of a [Patch].
     */
    val Class<out Patch<Context>>.description
        get() = findAnnotationRecursively(Description::class)?.description

    /**
     * The dependencies of a [Patch].
     */
    val Class<out Patch<Context>>.dependencies
        get() = findAnnotationRecursively(DependsOn::class)?.dependencies

    /**
     * The packages a [Patch] is compatible with.
     */
    val Class<out Patch<Context>>.compatiblePackages
        get() = findAnnotationRecursively(Compatibility::class)?.compatiblePackages

    /**
     * Weather or not a [Patch] requires integrations.
     */
    internal val Class<out Patch<Context>>.requiresIntegrations
        get() = findAnnotationRecursively(RequiresIntegrations::class) != null

    /**
     * The options of a [Patch].
     */
    val Class<out Patch<Context>>.options: PatchOptions?
        get() = kotlin.companionObject?.let { cl ->
            if (cl.visibility != KVisibility.PUBLIC) return null
            kotlin.companionObjectInstance?.let {
                (it as? OptionsContainer)?.options
            }
        }
}