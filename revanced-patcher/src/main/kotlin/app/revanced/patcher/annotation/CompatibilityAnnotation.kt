package app.revanced.patcher.annotation

import app.revanced.patcher.patch.Patch

/**
 * Annotation to constrain a [Patch] to compatible packages.
 * @param compatiblePackages A list of packages a [Patch] is compatible with.
 */
@Target(AnnotationTarget.CLASS)
annotation class Compatibility(
    val compatiblePackages: Array<Package>,
)

/**
 * Annotation to represent packages a patch can be compatible with.
 * @param name The package identifier name.
 * @param versions The versions of the package the [Patch] is compatible with.
 */
@Target()
annotation class Package(
    val name: String,
    val versions: Array<String> = [],
)
