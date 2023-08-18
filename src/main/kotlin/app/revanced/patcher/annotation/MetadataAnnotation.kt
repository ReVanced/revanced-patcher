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