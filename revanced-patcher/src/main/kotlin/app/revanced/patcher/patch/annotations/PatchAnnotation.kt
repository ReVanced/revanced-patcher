package app.revanced.patcher.patch.annotations

import app.revanced.patcher.Context
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
    val dependencies: Array<KClass<out Patch<Context>>> = [] // TODO: This should be a list of PatchClass instead
)


/**
 * Annotation to mark [Patch]es which depend on integrations.
 */
@Target(AnnotationTarget.CLASS)
annotation class RequiresIntegrations // TODO: Remove this annotation and replace it with a proper system