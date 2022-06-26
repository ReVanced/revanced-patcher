package app.revanced.patcher.fingerprint.method.annotation

import app.revanced.patcher.fingerprint.method.impl.MethodFingerprint

/**
 * Annotations for a method which matches to a [MethodFingerprint].
 * @param definingClass The defining class name of the method.
 * @param name A suggestive name for the method which the [MethodFingerprint] was created for.
 */
@Target(AnnotationTarget.CLASS)
@Retention(AnnotationRetention.RUNTIME)
annotation class MatchingMethod(
    val definingClass: String = "L<unspecified-class>;",
    val name: String = "<unspecified-method>"
)

/**
 * Annotations to scan a pattern [MethodFingerprint] with fuzzy algorithm.
 * @param threshold if [threshold] or more of the opcodes do not match, skip.
 */
@Target(AnnotationTarget.CLASS)
@Retention(AnnotationRetention.RUNTIME)
annotation class FuzzyPatternScanMethod(
    val threshold: Int = 1
)

/**
 * Annotations to scan a pattern [MethodFingerprint] directly.
 */
@Target(AnnotationTarget.CLASS)
@Retention(AnnotationRetention.RUNTIME)
annotation class DirectPatternScanMethod