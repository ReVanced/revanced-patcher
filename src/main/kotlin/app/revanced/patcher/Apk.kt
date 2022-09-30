package app.revanced.patcher

import app.revanced.patcher.data.PackageMetadata
import java.io.File

/**
 * The apk file that is to be patched.
 *
 * @param filePath The path to the apk file.
 */
sealed class Apk(filePath: String) {
    /**
     * The apk file.
     */
    val file = File(filePath)

    /**
     * The patched resources for the [Apk] given by the [Patcher].
     */
    var resources: File? = null
        internal set

    /**
     * The metadata of the [Apk].
     */
    val packageMetadata = PackageMetadata()

    /**
     * The split apk file that is to be patched.
     *
     * @param filePath The path to the apk file.
     * @see Apk
     */
    sealed class Split(filePath: String) : Apk(filePath) {
        /**
         * The split apk file which contains language files.
         *
         * @param filePath The path to the apk file.
         */
        class Language(filePath: String) : Split(filePath) {
            override fun toString() = "language"
        }

        /**
         * The split apk file which contains libraries.
         *
         * @param filePath The path to the apk file.
         */
        class Library(filePath: String) : Split(filePath) {
            override fun toString() = "library"
        }

        /**
         * The split apk file which contains assets.
         *
         * @param filePath The path to the apk file.
         */
        class Asset(filePath: String) : Split(filePath) {
            override fun toString() = "asset"
        }
    }

    /**
     * The base apk file that is to be patched.
     *
     * @param filePath The path to the apk file.
     * @see Apk
     */
    class Base(filePath: String) : Apk(filePath) {
        override fun toString() = "base"

        /**
         * The patched dex files for the [Base] apk returned by the [Patcher].
         */
        lateinit var dexFiles: List<app.revanced.patcher.util.dex.DexFile>
    }
}