package app.revanced.patcher.annotation

import app.revanced.patcher.patch.base.Patch
import app.revanced.patcher.signature.implementation.method.MethodSignature

/**
 * Annotation to name a [Patch] or [MethodSignature].
 * @param name A suggestive name for the [Patch] or [MethodSignature].
 */
@Target(AnnotationTarget.CLASS)
@Retention(AnnotationRetention.RUNTIME)
@MustBeDocumented
annotation class Name(
    val name: String,
)

/**
 * Annotation to describe a [Patch] or [MethodSignature].
 * @param description A description for the [Patch] or [MethodSignature].
 */
@Target(AnnotationTarget.CLASS)
@Retention(AnnotationRetention.RUNTIME)
@MustBeDocumented
annotation class Description(
    val description: String,
)


/**
 * Annotation to version a [Patch] or [MethodSignature].
 * @param version The version of a [Patch] or [MethodSignature].
 */
@Target(AnnotationTarget.CLASS)
@Retention(AnnotationRetention.RUNTIME)
@MustBeDocumented
annotation class Version(
    val version: String,
)
