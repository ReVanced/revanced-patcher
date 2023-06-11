package app.revanced.patcher.fingerprint.method.annotation

import app.revanced.patcher.fingerprint.method.impl.MethodFingerprint

/**
 * Annotations to scan a pattern [MethodFingerprint] with fuzzy algorithm.
 * @param threshold if [threshold] or more of the opcodes do not match, skip.
 */
@Target(AnnotationTarget.CLASS)
annotation class FuzzyPatternScanMethod(
    val threshold: Int = 1
)

@Target(AnnotationTarget.CLASS)
@Retention(AnnotationRetention.SOURCE)
annotation class ReferencedMethodDocumentation(
    val version: String,
    val className: String,
    val methodName: String,
    val parameterTypes: Array<String> = [],
    val returnType: String
)
