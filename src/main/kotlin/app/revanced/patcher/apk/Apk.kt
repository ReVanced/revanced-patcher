package app.revanced.patcher.apk

import app.revanced.patcher.PatcherOptions
import app.revanced.patcher.extensions.nullOutputStream
import brut.androlib.Androlib
import brut.androlib.ApkDecoder
import brut.androlib.meta.MetaInfo
import brut.androlib.meta.UsesFramework
import brut.androlib.options.BuildOptions
import brut.androlib.res.AndrolibResources
import brut.androlib.res.data.ResPackage
import brut.androlib.res.data.ResTable
import brut.androlib.res.decoder.AXmlResourceParser
import brut.androlib.res.decoder.ResAttrDecoder
import brut.androlib.res.decoder.XmlPullStreamDecoder
import brut.androlib.res.xml.ResXmlPatcher
import brut.directory.ExtFile
import brut.directory.ZipUtils
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
     * The patched resources for the [Apk] given by the [app.revanced.patcher.Patcher].
     */
    var resources: File? = null
        internal set

    /**
     * The metadata of the [Apk].
     */
    val packageMetadata = PackageMetadata()

    /**
     *  If the [Apk] has resources.
     */
    open val hasResources: Boolean = true

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
            // Library apks do not contain resources
            override val hasResources: Boolean = false

            override fun toString() = "library"

            /**
             * Write the resources for [Apk.Split.Library].
             *
             * @param resources Will be ignored.
             * @param patchApk The [Apk] file to write the resources to.
             * @param apkCacheDirectory The directory where the resources are stored.
             * @param metaInfo Will be ignored.
             */
            override fun writeResources(
                resources: AndrolibResources, patchApk: File, apkCacheDirectory: File, metaInfo: MetaInfo
            ) {
                // do not compress libraries for speed, because the patchApk is a temporal file
                val doNotCompress = apkCacheDirectory.listFiles()?.map { it.name }
                ZipUtils.zipFolders(apkCacheDirectory, patchApk, null, doNotCompress)
            }

            /**
             * Read resources for an [Apk] file.
             *
             * @param androlib The [Androlib] instance to decode the resources with.
             * @param extInputFile The [Apk] file.
             * @param outDir The directory to write the resources to.
             * @param resourceTable Will be ignored.
             */
            override fun readResources(
                androlib: Androlib, extInputFile: ExtFile, outDir: File, resourceTable: ResTable?
            ) {
                // only unpack raw files, such as libs
                androlib.decodeRawFiles(extInputFile, outDir, ApkDecoder.DECODE_ASSETS_NONE)
            }
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
         * The patched dex files for the [Base] apk returned by the [app.revanced.patcher.Patcher].
         */
        lateinit var dexFiles: List<app.revanced.patcher.util.dex.DexFile>
    }

    /**
     * Decode resources for a [Apk].
     * Note: This function respects the patchers [ResourceDecodingMode].
     *
     * @param options The [PatcherOptions] to decode the resources with.
     * @param mode The [ResourceDecodingMode] to use.
     */
    internal fun decodeResources(options: PatcherOptions, mode: ResourceDecodingMode) {
        val extInputFile = ExtFile(file)
        try {
            val androlib = Androlib(BuildOptions().also { it.setBuildOptions(options) })

            val resourceTable = androlib.getResTable(extInputFile, hasResources)
            when (mode) {
                ResourceDecodingMode.FULL -> {
                    val outDir = File(options.resourceCacheDirectory).resolve("resources").resolve(toString())
                        .also { it.mkdirs() }

                    readResources(androlib, extInputFile, outDir, resourceTable)

                    // read additional metadata from the resource table
                    with(packageMetadata) {
                        metaInfo.usesFramework = UsesFramework().also { framework ->
                            framework.ids = resourceTable.listFramePackages().map { it.id }.sorted()
                        }

                        // read files to not compress
                        metaInfo.doNotCompress = buildList {
                            androlib.recordUncompressedFiles(extInputFile, this)
                        }
                    }
                }
                ResourceDecodingMode.MANIFEST_ONLY -> {

                    // create decoder for the resource table
                    val decoder = ResAttrDecoder()
                    decoder.currentPackage = ResPackage(resourceTable, 0, null)

                    // create xml parser with the decoder
                    val axmlParser = AXmlResourceParser()
                    axmlParser.attrDecoder = decoder

                    // parse package information with the decoder and parser which will set required values in the resource table
                    // instead of decodeManifest another more low level solution can be created to make it faster/better
                    XmlPullStreamDecoder(
                        axmlParser, AndrolibResources().resXmlSerializer
                    ).decodeManifest(
                        extInputFile.directory.getFileInput("AndroidManifest.xml"), nullOutputStream
                    )
                }
            }

            // read of the resourceTable which is created by reading the manifest file
            with(packageMetadata) {
                packageName = resourceTable.currentResPackage.name
                packageVersion = resourceTable.versionInfo.versionName
                metaInfo.versionInfo = resourceTable.versionInfo
                metaInfo.sdkInfo = resourceTable.sdkInfo
            }
        } finally {
            extInputFile.close()
        }
    }

    /**
     * Read resources for an [Apk] file.
     *
     * @param androlib The [Androlib] instance to decode the resources with.
     * @param extInputFile The [Apk] file.
     * @param outDir The directory to write the resources to.
     * @param resourceTable The [ResTable] to use.
     */
    protected open fun readResources(
        androlib: Androlib, extInputFile: ExtFile, outDir: File, resourceTable: ResTable?
    ) {
        // always decode the manifest file
        androlib.decodeManifestWithResources(extInputFile, outDir, resourceTable)
        androlib.decodeResourcesFull(extInputFile, outDir, resourceTable)
    }


    /**
     * Compile resources for a [Apk].
     *
     * @param options The [PatcherOptions] to compile the resources with.
     */
    internal fun compileResources(options: PatcherOptions) {
        val packageMetadata = packageMetadata
        val metaInfo = packageMetadata.metaInfo

        val androlibResources = AndrolibResources().also { resources ->
            resources.buildOptions = BuildOptions().also { buildOptions ->
                buildOptions.setBuildOptions(options)
                buildOptions.isFramework = metaInfo.isFrameworkApk
                buildOptions.resourcesAreCompressed = metaInfo.compressionType
                buildOptions.doNotCompress = metaInfo.doNotCompress
            }

            resources.setSdkInfo(metaInfo.sdkInfo)
            resources.setVersionInfo(metaInfo.versionInfo)
            resources.setSharedLibrary(metaInfo.sharedLibrary)
            resources.setSparseResources(metaInfo.sparseResources)
        }

        val cacheDirectory = File(options.resourceCacheDirectory)

        // the resulting resource file
        val patchApk = cacheDirectory
            .resolve("patch")
            .also { it.mkdirs() }
            .resolve(file.name)
            .also { resources = it }

        val apkCacheDirectory = cacheDirectory.resolve("resources").resolve(toString())
        writeResources(androlibResources, patchApk, apkCacheDirectory, metaInfo)
    }

    /**
     * Write the resources for [Apk.file].
     *
     * @param resources The [AndrolibResources] to read the framework ids from.
     * @param patchApk The [Apk] file to write the resources to.
     * @param apkCacheDirectory The directory where the resources are stored.
     * @param metaInfo The [MetaInfo] for the [Apk] file.
     */
    protected open fun writeResources(
        resources: AndrolibResources, patchApk: File, apkCacheDirectory: File, metaInfo: MetaInfo
    ) = resources.aaptPackage(
        patchApk,
        apkCacheDirectory.resolve("AndroidManifest.xml")
            .also { ResXmlPatcher.fixingPublicAttrsInProviderAttributes(it) },
        apkCacheDirectory.resolve("res"),
        null,
        null,
        metaInfo.usesFramework.ids.map { id ->
            resources.getFrameworkApk(
                id, metaInfo.usesFramework.tag
            )
        }.toTypedArray()
    )

    private companion object {
        /**
         * Set options for the [Androlib] instance.
         *
         * @param options The [PatcherOptions].
         */
        fun BuildOptions.setBuildOptions(options: PatcherOptions) {
            this.aaptPath = options.aaptPath
            this.useAapt2 = true
            this.frameworkFolderLocation = options.frameworkPath
        }
    }

    /**
     * The type of decoding the resources.
     */
    internal enum class ResourceDecodingMode {
        /**
         * Decode all resources.
         */
        FULL,

        /**
         * Decode the manifest file only.
         */
        MANIFEST_ONLY,
    }

    /**
     * Metadata about an [Apk] file.
     */
    class PackageMetadata {
        /**
         * The [MetaInfo] of the [Apk] file.
         */
        internal val metaInfo: MetaInfo = MetaInfo()

        /**
         * List of [Apk] files which should remain uncompressed.
         */
        val doNotCompress: Collection<String>
            get() = metaInfo.doNotCompress

        /**
         * The package name of the [Apk] file.
         */
        var packageName: String? = null
            internal set

        /**
         * The package version of the [Apk] file.
         *
         * Note: __null__ for [Apk.Split.Library].
         */
        var packageVersion: String? = null
            internal set
    }
}