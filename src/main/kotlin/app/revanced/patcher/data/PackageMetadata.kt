package app.revanced.patcher.data

import brut.androlib.meta.MetaInfo

/**
 * Metadata about a package.
 */
class PackageMetadata {
    internal val metaInfo: MetaInfo = MetaInfo()
    lateinit var packageName: String
    lateinit var packageVersion: String

    val doNotCompress = metaInfo.doNotCompress
}
