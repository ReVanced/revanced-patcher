package app.revanced.patcher.patch.annotations

import app.revanced.patcher.data.Context
import app.revanced.patcher.patch.Patch
import kotlin.reflect.KClass

/**
 * Annotation to mark a class as a patch.
 * @param include If false, the patch should be treated as optional by default.
 */
@Target(AnnotationTarget.CLASS)
annotation class Patch(val include: Boolean = true)

/**
 * Annotation for dependencies of [Patch]es.
 */
@Target(AnnotationTarget.CLASS)
annotation class DependsOn(
    val dependencies: Array<KClass<out Patch<Context<*>>>> = []
)

// TODO: Remove this annotation, once integrations are coupled with patches.
/**
 * Annotation to mark [Patch]es which depend on integrations.
 */
@Target(AnnotationTarget.CLASS)
annotation class RequiresIntegrations