package app.revanced.patcher.patch.annotations

/**
 * Annotation to mark a Class as a patch.
 */
@Target(AnnotationTarget.CLASS)
@Retention(AnnotationRetention.RUNTIME)
@MustBeDocumented
annotation class Patch