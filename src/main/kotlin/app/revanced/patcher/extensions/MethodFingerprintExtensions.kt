package app.revanced.patcher.extensions

import app.revanced.patcher.fingerprint.MethodFingerprint
import app.revanced.patcher.fingerprint.annotation.FuzzyPatternScanMethod

object MethodFingerprintExtensions {
    /**
     * The [FuzzyPatternScanMethod] annotation of a [MethodFingerprint].
     */
    @Suppress("EXTENSION_SHADOWED_BY_MEMBER")
    @Deprecated(
        message = "Use the property instead.",
        replaceWith = ReplaceWith("this.fuzzyPatternScanMethod"),
    )
    val MethodFingerprint.fuzzyPatternScanMethod
        get() = this.fuzzyPatternScanMethod
}
