package app.revanced.patcher

import java.io.File

/**
 * An APK to patch.
 *
 * @param file The base APK file. This is the source of truth for the manifest, package metadata, and primary dex files.
 */
sealed class Apk(val file: File) {
    /**
     * A single monolithic APK.
     *
     * @param file The APK file.
     */
    class Single(file: File) : Apk(file)

    /**
     * A split APK bundle.
     *
     * @param baseApk The base APK file.
     * @param splitApkFiles The split APK files, keyed by split name.
     */
    class Split(
        baseApk: File,
        val splitApkFiles: Map<String, File>,
    ) : Apk(baseApk)
}
