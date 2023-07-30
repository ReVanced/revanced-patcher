package app.revanced.patcher.data

import brut.androlib.apk.ApkInfo

/**
 * Metadata about a package.
 */
class PackageMetadata {
    lateinit var packageName: String
        internal set
    lateinit var packageVersion: String
        internal set

    internal lateinit var apkInfo: ApkInfo
}
