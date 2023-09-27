package app.revanced.patcher.patch.annotation

import java.lang.annotation.Inherited
import kotlin.reflect.KClass

/**
 * Annotation for [app.revanced.patcher.patch.Patch] classes.
 *
 * @param name The name of the patch. If empty, the patch will be unnamed.
 * @param description The description of the patch. If empty, no description will be used.
 * @param dependencies The patches this patch depends on.
 * @param compatiblePackages The packages this patch is compatible with.
 * @param use Whether this patch should be used.
 * @param requiresIntegrations Whether this patch requires integrations.
 */
@Retention(AnnotationRetention.SOURCE)
@Target(AnnotationTarget.CLASS)
@Inherited
annotation class Patch(
    val name: String = "",
    val description: String = "",
    val dependencies: Array<KClass<out app.revanced.patcher.patch.Patch<*>>> = [],
    val compatiblePackages: Array<CompatiblePackage> = [],
    val use: Boolean = true,
    // TODO: Remove this property, once integrations are coupled with patches.
    val requiresIntegrations: Boolean = false,
)

/**
 * A package that a [app.revanced.patcher.patch.Patch] is compatible with.
 *
 * @param name The name of the package.
 * @param versions The versions of the package.
 */
annotation class CompatiblePackage(
    val name: String,
    val versions: Array<String> = [],
)