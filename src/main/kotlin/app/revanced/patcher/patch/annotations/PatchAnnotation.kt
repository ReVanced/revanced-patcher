package app.revanced.patcher.patch.annotations

import app.revanced.patcher.data.base.Data
import kotlin.reflect.KClass

/**
 * Annotation to mark a Class as a patch.
 */
@Target(AnnotationTarget.CLASS)
@Retention(AnnotationRetention.RUNTIME)
@MustBeDocumented
annotation class Patch(
    val dependencies: Array<KClass<out app.revanced.patcher.patch.base.Patch<Data>>> = []
)
