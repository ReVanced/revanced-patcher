package app.revanced.patcher.annotation

import app.revanced.patcher.fingerprint.method.impl.MethodFingerprint
import app.revanced.patcher.patch.Patch

/**
 * Annotation to constrain a [Patch] or [MethodFingerprint] to compatible packages.
 * @param compatiblePackages A list of packages a [Patch] or [MethodFingerprint] is compatible with.
 */
@Target(AnnotationTarget.CLASS)
@Retention(AnnotationRetention.RUNTIME)
@MustBeDocumented
annotation class Compatibility(
    val compatiblePackages: Array<Package>,
)

/**
 * Annotation to represent packages a patch can be compatible with.
 * @param name The package identifier name.
 * @param versions The versions of the package the [Patch] or [MethodFingerprint]is compatible with.
 */
@Target()
@Retention(AnnotationRetention.RUNTIME)
@MustBeDocumented
annotation class Package(
    val name: String,
    val versions: Array<String>
)