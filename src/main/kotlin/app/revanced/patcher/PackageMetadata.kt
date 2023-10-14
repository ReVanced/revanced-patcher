package app.revanced.patcher

import brut.androlib.apk.ApkInfo

/**
 * Metadata about a package.
 */
class PackageMetadata internal constructor(internal val apkInfo: ApkInfo) {
    lateinit var packageName: String
        internal set

    lateinit var packageVersion: String
        internal set
}
