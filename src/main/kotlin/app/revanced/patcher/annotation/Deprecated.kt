package app.revanced.patcher.annotation

import app.revanced.patcher.Context
import app.revanced.patcher.patch.Patch
import kotlin.reflect.KClass

/**
 * Declares a [Patch] deprecated for removal.
 * @param reason The reason why the patch is deprecated.
 * @param replacement The replacement for the deprecated patch, if any.
 */
@Target(AnnotationTarget.CLASS)
@Retention(AnnotationRetention.RUNTIME)
@MustBeDocumented
annotation class PatchDeprecated(
    val reason: String,
    val replacement: KClass<out Patch<Context>> = Patch::class
    // Values cannot be nullable in annotations, so this will have to do.
)