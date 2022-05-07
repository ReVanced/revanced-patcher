package app.revanced.patcher.patch.implementation.metadata

import app.revanced.patcher.patch.base.Patch

/**
 * Metadata about a [Patch].
 * @param shortName A suggestive short name for the [Patch].
 * @param name A suggestive name for the [Patch].
 * @param description A description for the [Patch].
 * @param compatiblePackages A list of packages this [Patch] is compatible with.
 * @param version The version of the [Patch].
 */
data class PatchMetadata(
    val shortName: String,
    val name: String,
    val description: String,
    val compatiblePackages: Iterable<PackageMetadata>,
    val version: String,
)

/**
 * Metadata about a package.
 * @param name The package name.
 * @param versions Compatible versions of the package.
 */
data class PackageMetadata(
    val name: String,
    val versions: Iterable<String>
)