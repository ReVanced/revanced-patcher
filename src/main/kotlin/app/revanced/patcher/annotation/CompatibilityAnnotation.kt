package app.revanced.patcher.annotation

import app.revanced.patcher.patch.base.Patch
import app.revanced.patcher.signature.implementation.method.MethodSignature

/**
 * Annotation to constrain a [Patch] or [MethodSignature] to compatible packages.
 * @param compatiblePackages A list of packages a [Patch] or [MethodSignature] is compatible with.
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
 * @param versions The versions of the package the [Patch] or [MethodSignature]is compatible with.
 */
@Target()
@Retention(AnnotationRetention.RUNTIME)
@MustBeDocumented
annotation class Package(
    val name: String,
    val versions: Array<String>
)