package app.revanced.patcher.extensions

import app.revanced.patcher.extensions.AnnotationExtensions.findAnnotationRecursively
import app.revanced.patcher.fingerprint.method.annotation.FuzzyPatternScanMethod
import app.revanced.patcher.fingerprint.method.impl.MethodFingerprint

object MethodFingerprintExtensions {

    /**
     * The name of a [MethodFingerprint].
     */
    val MethodFingerprint.name: String
        get() = this.javaClass.simpleName

    /**
     * The [FuzzyPatternScanMethod] annotation of a [MethodFingerprint].
     */
    val MethodFingerprint.fuzzyPatternScanMethod
        get() = javaClass.findAnnotationRecursively(FuzzyPatternScanMethod::class)
}