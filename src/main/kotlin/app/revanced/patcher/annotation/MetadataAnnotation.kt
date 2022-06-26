package app.revanced.patcher.annotation

import app.revanced.patcher.fingerprint.method.impl.MethodFingerprint
import app.revanced.patcher.patch.Patch

/**
 * Annotation to name a [Patch] or [MethodFingerprint].
 * @param name A suggestive name for the [Patch] or [MethodFingerprint].
 */
@Target(AnnotationTarget.CLASS)
@Retention(AnnotationRetention.RUNTIME)
@MustBeDocumented
annotation class Name(
    val name: String,
)

/**
 * Annotation to describe a [Patch] or [MethodFingerprint].
 * @param description A description for the [Patch] or [MethodFingerprint].
 */
@Target(AnnotationTarget.CLASS)
@Retention(AnnotationRetention.RUNTIME)
@MustBeDocumented
annotation class Description(
    val description: String,
)


/**
 * Annotation to version a [Patch] or [MethodFingerprint].
 * @param version The version of a [Patch] or [MethodFingerprint].
 */
@Target(AnnotationTarget.CLASS)
@Retention(AnnotationRetention.RUNTIME)
@MustBeDocumented
annotation class Version(
    val version: String,
)
