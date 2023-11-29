package app.revanced.patcher.extensions

import app.revanced.patcher.fingerprint.MethodFingerprint
import app.revanced.patcher.fingerprint.annotation.FuzzyPatternScanMethod

object MethodFingerprintExtensions {
    /**
     * The [FuzzyPatternScanMethod] annotation of a [MethodFingerprint].
     */
    @Deprecated(
        message = "Use the property instead.",
        replaceWith = ReplaceWith("this.fuzzyPatternScanMethod"),
    )
    val MethodFingerprint.fuzzyPatternScanMethod
        get() = this.fuzzyPatternScanMethod
}
