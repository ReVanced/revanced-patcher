package app.revanced.patcher.annotation

import app.revanced.patcher.patch.Patch

/**
 * Annotation to name a [Patch].
 * @param name A suggestive name for the [Patch].
 */
@Target(AnnotationTarget.CLASS)
annotation class Name(
    val name: String,
)

/**
 * Annotation to describe a [Patch].
 * @param description A description for the [Patch].
 */
@Target(AnnotationTarget.CLASS)
annotation class Description(
    val description: String,
)


/**
 * Annotation to version a [Patch].
 * @param version The version of a [Patch].
 */
@Target(AnnotationTarget.CLASS)
@Deprecated("This annotation is deprecated and will be removed in the future.")
annotation class Version(
    val version: String,
)
