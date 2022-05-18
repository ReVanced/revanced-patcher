package app.revanced.patcher.signature.implementation.method.annotation

import app.revanced.patcher.signature.implementation.method.MethodSignature

/**
 * Annotations for a method which matches to a [MethodSignature].
 * @param definingClass The defining class name of the method.
 * @param name A suggestive name for the method which the [MethodSignature] was created for.
 */
@Target(AnnotationTarget.CLASS)
@Retention(AnnotationRetention.RUNTIME)
annotation class MatchingMethod(
    val definingClass: String = "L<empty>",
    val name: String = "<method>"
)

/**
 * Annotations to scan a pattern [MethodSignature] with fuzzy algorithm.
 * @param threshold if [threshold] or more of the opcodes do not match, skip.
 */
@Target(AnnotationTarget.CLASS)
@Retention(AnnotationRetention.RUNTIME)
annotation class FuzzyPatternScanMethod(
    val threshold: Int = 1
)

/**
 * Annotations to scan a pattern [MethodSignature] directly.
 */
@Target(AnnotationTarget.CLASS)
@Retention(AnnotationRetention.RUNTIME)
annotation class DirectPatternScanMethod