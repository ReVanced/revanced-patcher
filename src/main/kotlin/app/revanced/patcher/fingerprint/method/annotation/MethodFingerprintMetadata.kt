package app.revanced.patcher.fingerprint.method.annotation

import app.revanced.patcher.fingerprint.method.impl.MethodFingerprint

/**
 * Annotations to scan a pattern [MethodFingerprint] with fuzzy algorithm.
 * @param threshold if [threshold] or more of the opcodes do not match, skip.
 */
@Target(AnnotationTarget.CLASS)
@Retention(AnnotationRetention.RUNTIME)
annotation class FuzzyPatternScanMethod(
    val threshold: Int = 1
)