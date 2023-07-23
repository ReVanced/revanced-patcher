package app.revanced.patcher.data

import brut.androlib.apk.ApkInfo

/**
 * Metadata about a package.
 */
class PackageMetadata {
    lateinit var packageName: String
    lateinit var packageVersion: String

    internal val apkInfo: ApkInfo = ApkInfo()
}
