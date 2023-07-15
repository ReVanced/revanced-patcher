package app.revanced.patcher

import app.revanced.patcher.apk.Apk
import java.io.File

/**
 * The result of a patcher.
 * @param apkFiles The patched [Apk] files.
 */
data class PatcherResult(val apkFiles: List<Patch>) {

    /**
     * The result of a patch.
     *
     * @param apk The patched [Apk] file.
     */
    sealed class Patch(val apk: Apk) {

        /**
         * The result of a patch of an [Apk.Split] file.
         *
         * @param apk The patched [Apk.Split] file.
         */
        class Split(apk: Apk.Split) : Patch(apk)

        /**
         * The result of a patch of an [Apk.Split] file.
         *
         * @param apk The patched [Apk.Base] file.
         */
        class Base(apk: Apk.Base) : Patch(apk)
    }
}