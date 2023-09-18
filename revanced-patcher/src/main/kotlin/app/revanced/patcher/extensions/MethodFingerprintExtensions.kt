package app.revanced.patcher.extensions

import app.revanced.patcher.extensions.AnnotationExtensions.findAnnotationRecursively
import app.revanced.patcher.fingerprint.method.annotation.FuzzyPatternScanMethod
import app.revanced.patcher.fingerprint.method.impl.MethodFingerprint

object MethodFingerprintExtensions {
    // TODO: Make this a property.
    /**
     * The [FuzzyPatternScanMethod] annotation of a [MethodFingerprint].
     */
    val MethodFingerprint.fuzzyPatternScanMethod
        get() = javaClass.findAnnotationRecursively(FuzzyPatternScanMethod::class)
}