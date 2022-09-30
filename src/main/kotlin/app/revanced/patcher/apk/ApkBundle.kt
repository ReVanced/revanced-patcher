package app.revanced.patcher.apk

import app.revanced.patcher.PatcherOptions

/**
 * An [Apk] file of type [Apk.Split].
 *
 * @param base The apk file of type [Apk.Base].
 * @param split The [Apk.Split] files.
 */
class ApkBundle(
    val base: Apk.Base,
    split: Split? = null
) {
    /**
     * The [Apk.Split] files.
     */
    var split = split
        internal set

    /**
     * Merge all [Apk.Split] files to [Apk.Base].
     * This will set [split] to null.
     * @param options The [PatcherOptions] to write the resources with.
     */
    internal fun mergeResources(options: PatcherOptions) {
        split?.let { base.mergeSplitResources(it, options) }
        split = null
    }

    /**
     * Write resources for the files in [ApkBundle].
     *
     * @param options The [PatcherOptions] to write the resources with.
     * @return A sequence of the [Apk] files which resources are being written.
     */
    internal fun writeResources(options: PatcherOptions) = sequence {
        with(base) {
            writeResources(options)

            yield(SplitApkResult.Write(this))
        }

        split?.all?.forEach { splitApk ->
            with(splitApk) {
                var exception: Apk.ApkException.Write? = null

                try {
                    writeResources(options)
                } catch (writeException: Apk.ApkException.Write) {
                    exception = writeException
                }

                yield(SplitApkResult.Write(this, exception))
            }
        }
    }

    /**
     * Decode resources for the files in [ApkBundle].
     *
     * @param options The [PatcherOptions] to decode the resources with.
     * @param mode The [Apk.ResourceDecodingMode] to use.
     * @return A sequence of the [Apk] files which resources are being decoded.
     */
    internal fun decodeResources(options: PatcherOptions, mode: Apk.ResourceDecodingMode) = sequence {
        with(base) {
            yield(this)
            decodeResources(options, mode)
        }

        split?.all?.forEach {
            yield(it)
            it.decodeResources(options, mode)
        }
    }


    /**
     * Class for [Apk.Split].
     *
     * @param library The apk file of type [Apk.Base].
     * @param asset The apk file of type [Apk.Base].
     * @param language The apk file of type [Apk.Base].
     */
    class Split(
        library: Apk.Split.Library,
        asset: Apk.Split.Asset,
        language: Apk.Split.Language
    ) {
        var library = library
            internal set
        var asset = asset
            internal set
        var language = language
            internal set

        val all get() = listOfNotNull(library, asset, language)
    }

    /**
     * The result of writing a [Split] [Apk] file.
     *
     * @param apk The corresponding [Apk] file.
     * @param exception The optional [Apk.ApkException] when an exception occurred.
     */
    sealed class SplitApkResult(val apk: Apk, val exception: Apk.ApkException? = null) {
        /**
         * The result of writing a [Split] [Apk] file.
         *
         * @param apk The corresponding [Apk] file.
         * @param exception The optional [Apk.ApkException] when an exception occurred.
         */
        class Write(apk: Apk, exception: Apk.ApkException.Write? = null) : SplitApkResult(apk, exception)
    }
}