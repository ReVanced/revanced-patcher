package app.revanced.patcher.patch.annotations

import app.revanced.patcher.data.base.Data
import app.revanced.patcher.patch.base.Patch
import kotlin.reflect.KClass

/**
 * Annotation to mark a Class as a patch.
 */
@Target(AnnotationTarget.CLASS)
@Retention(AnnotationRetention.RUNTIME)
@MustBeDocumented
annotation class Patch(val excludeByDefault: Boolean = false)

/**
 * Annotation for dependencies of [Patch]es .
 */
@Target(AnnotationTarget.CLASS)
@Retention(AnnotationRetention.RUNTIME)
@MustBeDocumented
annotation class Dependencies(
    val dependencies: Array<KClass<out Patch<Data>>> = []
)