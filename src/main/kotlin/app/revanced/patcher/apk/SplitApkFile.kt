package app.revanced.patcher.apk

import app.revanced.patcher.PatcherOptions

/**
 * An [Apk] file of type [Apk.Split].
 *
 * @param base The apk file of type [Apk.Base].
 * @param splits The apk files of type [Apk.Split].
 */
data class SplitApkFile(val base: Apk.Base, val splits: List<Apk.Split> = emptyList()) {
    /**
     * Compile resources for the files in [SplitApkFile].
     *
     * @param options The [PatcherOptions] to compile the resources with.
     */
    internal fun compileResources(options: PatcherOptions) {
        base.compileResources(options)
        splits.forEach { it.compileResources(options) }
    }

    /**
     * Decode resources for the files in [SplitApkFile].
     *
     * @param options The [PatcherOptions] to decode the resources with.
     * @param mode The [Apk.ResourceDecodingMode] to use.
     */
    internal fun decodeResources(options: PatcherOptions, mode: Apk.ResourceDecodingMode) {
        base.decodeResources(options, mode)
        splits.forEach { it.decodeResources(options, mode) }
    }
}