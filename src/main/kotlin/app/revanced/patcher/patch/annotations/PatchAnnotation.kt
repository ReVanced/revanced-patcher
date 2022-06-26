package app.revanced.patcher.patch.annotations

import app.revanced.patcher.data.Data
import app.revanced.patcher.patch.Patch
import kotlin.reflect.KClass

/**
 * Annotation to mark a Class as a patch.
 * @param include If false, the patch should be treated as optional by default.
 */
@Target(AnnotationTarget.CLASS)
@Retention(AnnotationRetention.RUNTIME)
@MustBeDocumented
annotation class Patch(val include: Boolean = true)

/**
 * Annotation for dependencies of [Patch]es .
 */
@Target(AnnotationTarget.CLASS)
@Retention(AnnotationRetention.RUNTIME)
@MustBeDocumented
annotation class Dependencies(
    val dependencies: Array<KClass<out Patch<Data>>> = []
)