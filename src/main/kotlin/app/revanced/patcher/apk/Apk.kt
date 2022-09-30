@file:Suppress("MemberVisibilityCanBePrivate")

package app.revanced.patcher.apk

import app.revanced.patcher.DomFileEditor
import app.revanced.patcher.Patcher
import app.revanced.patcher.PatcherOptions
import app.revanced.patcher.extensions.nullOutputStream
import app.revanced.patcher.util.ProxyBackedClassList
import app.revanced.patcher.util.dex.DexFile
import app.revanced.patcher.util.dom.DomUtil.doRecursively
import brut.androlib.Androlib
import brut.androlib.AndrolibException
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
import lanchon.multidexlib2.DexIO
import lanchon.multidexlib2.MultiDexIO
import org.jf.dexlib2.Opcodes
import org.jf.dexlib2.writer.io.MemoryDataStore
import org.w3c.dom.*
import java.io.File
import java.nio.file.FileSystems
import java.nio.file.Files
import kotlin.io.path.copyTo

/**
 * The apk file that is to be patched.
 *
 * @param filePath The path to the apk file.
 */
sealed class Apk(filePath: String) {
    /**
     * The apk file.
     */
    open val file = File(filePath)

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
     * Get the resource directory of the apk file.
     *
     * @param options The patcher context to resolve the resource directory for the [Apk] file.
     * @return The resource directory of the [Apk] file.
     */
    protected fun getResourceDirectory(options: PatcherOptions) = options.resourceDirectory.resolve(toString())

    /**
     * Get a file from the resources of the [Apk] file.
     *
     * @param path The path of the resource file.
     * @param options The patcher context to resolve the resource directory for the [Apk] file.
     * @return A [File] instance for the resource file.
     */
    internal fun getFile(path: String, options: PatcherOptions) = getResourceDirectory(options).resolve(path)

    /**
     * Transform public drawable resource references.
     *
     * @param options The patcher context to resolve the resource directory for the [Apk] file.
     * @param transformer The callback with [Document] for public.xml to execute.
     */
    protected fun transformDrawableReferences(options: PatcherOptions, transformer: (Document) -> Unit) {
        getResourceDirectory(options).resolve("res/values/public.xml").apply {
            if (!exists()) return

            DomFileEditor(this).use { editor ->
                transformer(editor.file)
            }
        }
    }

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

            /**
             * Write the resources for [Apk.Split.Library].
             *
             * @param resources Will be ignored.
             * @param patchApk The [Apk] file to write the resources to.
             * @param apkWorkDirectory The directory where the resources are stored.
             * @param metaInfo Will be ignored.
             */
            override fun writeResources(
                resources: AndrolibResources, patchApk: File, apkWorkDirectory: File, metaInfo: MetaInfo
            ) {
                // Do not compress libraries (.so) for speed, because the patchApk is a temporal file.
                ZipUtils.zipFolders(apkWorkDirectory, patchApk, null, listOf("so"))

                // Write the patchApk file containing the manifest file.
                apkWorkDirectory.resolve(patchApk.name).also { manifestPatchApk ->
                    super.writeResources(resources, manifestPatchApk, apkWorkDirectory, metaInfo)
                }.let { manifestPatchApk ->
                    // Copy AndroidManifest.xml from manifestPatchApk to patchApk.
                    fun File.createFs() = FileSystems.newFileSystem(toPath(), null as ClassLoader?)
                    manifestPatchApk.createFs().use { manifestPatchApkFs ->
                        patchApk.createFs().use { patchApkFs ->
                            // Delete AndroidManifest.xml from patchApk and copy it from manifestPatchApk.
                            patchApkFs.getPath("/AndroidManifest.xml").also { manifestPath ->
                                patchApkFs.provider().delete(manifestPath)
                                manifestPatchApkFs.getPath("/AndroidManifest.xml").copyTo(manifestPath)
                            }
                        }
                    }
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
            override fun readResources(
                androlib: Androlib, extInputFile: ExtFile, outDir: File, resourceTable: ResTable?
            ) {
                // Decode the manifest without looking up attribute references because there is no resources.arsc file.
                androlib.decodeManifestFull(extInputFile, outDir, resourceTable)
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
        /**
         * Data of the [Base] apk file.
         */
        internal val bytecodeData = BytecodeData()

        /**
         * The patched dex files for the [Base] apk file.
         */
        lateinit var dexFiles: List<DexFile>
            internal set

        /**
         * Additional files which are now in [Apk.Base] from calling [mergeSplitResources].
         */
        var mergedResources: List<File>? = null
            internal set

        override fun toString() = "base"

        /**
         * Move resources from an [ApkBundle] to the [Base] [Apk] file.
         *
         * @param splits The [ApkBundle.Split]s to move resources from.
         * @param options The [PatcherOptions] of the [Patcher].
         */
        internal fun mergeSplitResources(splits: ApkBundle.Split, options: PatcherOptions) {
            val baseResourceDirectory = getResourceDirectory(options)

            // region Prepare the base apk file

            // Remove the attribute isSplitRequired.
            DomFileEditor(baseResourceDirectory.resolve("AndroidManifest.xml")).use { editor ->
                val element = editor.file.getElementsByTagName("application").item(0) as Element
                // If no attribute with this name is found, this method has no effect.
                arrayOf("android:isSplitRequired", "android:extractNativeLibs").forEach(element::removeAttribute)
            }

            // Merge doNotCompress lists from all splits to base
            splits.all.onEach { split ->
                split.packageMetadata.doNotCompress?.let { list ->
                    packageMetadata.metaInfo.apply {
                        doNotCompress = doNotCompress.toMutableList().also { it.addAll(list) }.distinct()
                    }
                }
            }

            // endregion

            /**
             * Merge split resources to the base resources.
             *
             * In this process all references to drawables made in the base apk resources have to be fixed.
             * For that the public.xml file is used to find the resource names for each resource reference
             * through its resource id in the assets split apk resources.
             *
             * 1. Initial state of the resources:
             *
             *     base/res/values/public.xml:
             *          <!--...-->
             *          <public type="drawable" name="APKTOOL_DUMMY_7ce" id="0x7f0807ce" />
             *          <public type="drawable" name="APKTOOL_DUMMY_7cf" id="0x7f0807cf" />
             *          <public type="drawable" name="APKTOOL_DUMMY_7d0" id="0x7f0807d0" />
             *          <!--...-->
             *
             *     base/res/layout/playlist_editor_collaboration_section_fragment.xml:
             *          <!--...-->
             *          <SomeElement someAttribute="@drawable/APKTOOL_DUMMY_7ce"/>
             *          <!--...-->
             *
             *     asset/res/values/public.xml
             *         <!--...-->
             *         <public type="drawable" name="quantum_ic_exit_to_app_grey600_24" id="0x7f0807ce" />
             *         <public type="drawable" name="APKTOOL_DUMMY_7cf" id="0x7f0807cf" />
             *         <public type="drawable" name="quantum_ic_expand_more_grey600_18" id="0x7f0807d0" />
             *         <!--...-->
             *
             * 2. Merge all split apk resources into base, ignoring existing files.
             *
             * 3. Fix all APKTOOL_DUMMY_* resource references in base/res/:
             *
             *     base/res/values/public.xml:
             *          <!--...-->
             *          <public type="drawable" name="quantum_ic_exit_to_app_grey600_24" id="0x7f0807ce" />
             *          <public type="drawable" name="APKTOOL_DUMMY_7cf" id="0x7f0807cf" />
             *          <public type="drawable" name="quantum_ic_expand_more_grey600_18" id="0x7f0807d0" />
             *          <!--...-->
             *
             *     base/res/layout/playlist_editor_collaboration_section_fragment.xml:
             *          <!--...-->
             *          <SomeElement someAttribute="@drawable/quantum_ic_exit_to_app_grey600_24"/>
             *          <!--...-->
             *
             * 4. At last, all remaining dummy APKTOOL_DUMMY_* resource references have to be removed:
             *
             *     base/res/values/public.xml:
             *          <!--...-->
             *          <public type="drawable" name="quantum_ic_exit_to_app_grey600_24" id="0x7f0807ce" />
             *          <public type="drawable" name="quantum_ic_expand_more_grey600_18" id="0x7f0807d0" />
             *          <!--...-->
             *
             *     base/res/layout/playlist_editor_collaboration_section_fragment.xml:
             *          <!--...-->
             *          <SomeElement someAttribute="@drawable/quantum_ic_exit_to_app_grey600_24"/>
             *          <!--...-->
             *
             */

            // region Helper functions

            /**
             * Transform a directory recursively relative to another.
             *
             * @param to The directory to recursively transform the directory which this function is called on.
             * @param transformer The function to call for each [File].
             */
            fun File.transform(to: File? = null, transformer: (File, File?) -> Unit) {
                listFiles()?.forEach { file ->
                    val newFile = to?.resolve(file.name)

                    if (file.isDirectory) file.transform(newFile, transformer)
                    else transformer(file, newFile)
                }
            }

            /**
             * Move a directory recursively into another.
             *
             * @param to The directory to move to.
             */
            fun File.moveTo(to: File) = transform(to) { from, to ->
                if (to!!.exists()) return@transform
                // Make sure the directory exists where we want to move the file to.
                to.parentFile.apply {
                    if (!exists()) mkdirs()
                    Files.move(from.toPath(), to.toPath())
                }
            }

            /**
             * Call [callback] with each [Element] with the tag "public".
             *
             * @param callback The callback function to call for each [Element].
             */
            fun Apk.forEachPublicElement(callback: (Element) -> Unit) {
                fun Document.forEachPublicNode(callback: (Element) -> Unit) {
                    getElementsByTagName("public").apply {
                        for (i in 0 until length) {
                            val element = item(i) as? Element ?: continue

                            callback(element)
                        }
                    }
                }

                transformDrawableReferences(options) { editor ->
                    editor.forEachPublicNode { element -> callback(element) }
                }
            }

            // endregion

            // region Fix resource references

            // Move split resources to base.
            splits.all.forEach { split ->
                split.getResourceDirectory(options).also {
                    // Except for Split.Library, all splits use the /res resource directory.
                    // For that reason Base.mergedResources has to be set
                    // with the files which are merged from Split.Library to Apk.Base.
                    // TODO: This is a flawed implementation because this comment assumed.
                    if (split is Split.Library) this@Base.mergedResources = it.listFiles()
                        ?.filter(File::isDirectory)
                        ?.map { file ->
                            baseResourceDirectory.resolve(file.name)
                        }
                }.moveTo(baseResourceDirectory)
            }

            // Collect a map of the old dummy and original resource names.
            val dummyOriginalResources = buildMap {
                // Collect drawable resources which are referred in the base apk resources.
                val originalResources = buildMap references@{
                    splits.asset.forEachPublicElement { element ->
                        val name = element.getAttribute("name")

                        // Skip elements which are dummy references,
                        // because only those with the original name are required.
                        if (name.startsWith(DUMMY_RESOURCE_NAME_PREFIX)) return@forEachPublicElement

                        references@ put(
                            element.getAttribute("id"), name
                        )
                    }
                }

                forEachPublicElement { element ->
                    val id = element.getAttribute("id")
                    val dummyName = element.getAttribute("name")

                    originalResources[id]?.let { originalName -> put(dummyName, originalName) }
                }
            }

            // TODO: DomFileEditor is slow, consider using manual text manipulation instead.

            // Rename the dummy attribute name of each resource reference in all xml resources.
            baseResourceDirectory.transform { file, _ ->
                // Skip all non relevant files.
                if (file.extension != "xml") return@transform

                DomFileEditor(file).use { editor ->
                    /**
                     * Replace dummy resource names in the [Node] node value.
                     *
                     * If no substitution can be found, [notFoundCallback] will be called.
                     * If [DUMMY_RESOURCE_NAME_PREFIX] could not be found in the node value, nothing will change.
                     *
                     * @param notFoundCallback The function to call when no substitution can be found.
                     */
                    fun Node.transformDummyValue(notFoundCallback: (() -> Unit)) {
                        val value = nodeValue
                        // Check if the node value contains the required prefix.
                        val index = value.indexOf(DUMMY_RESOURCE_NAME_PREFIX)
                        if (index == -1) return

                        // android:drawable/DUMMY_RESOURCE_NAME_PREFIX_00 -> android:drawable/FULL_QUALIFIER_NAME
                        dummyOriginalResources[value.substring(index)]?.let { name ->
                            nodeValue = value.replaceRange(index, value.length, name)
                        } ?: notFoundCallback()
                    }

                    // Replace dummy resource names for each element,
                    // removing those elements which dummy name could not be resolved.
                    editor.file.doRecursively { node ->
                        if (node !is Element) return@doRecursively

                        // Replace values of attributes referencing dummy resource names.
                        node.attributes?.apply {
                            for (i in length - 1 downTo 0) {
                                val attribute = item(i)
                                attribute.transformDummyValue {
                                    node.parentNode?.removeChild(node)
                                }
                            }
                        }

                        // Replace values of text elements referencing dummy resource names.
                        node.childNodes.apply {
                            if (length != 1) return@doRecursively

                            val text = item(0) as? Text ?: return@doRecursively
                            text.transformDummyValue {
                                node.removeChild(text)
                            }
                        }
                    }
                }
            }

            // endregion
        }
    }

    internal inner class BytecodeData {
        private val opcodes: Opcodes

        /**
         * The classes and proxied classes of the [Base] apk file.
         */
        val classes = ProxyBackedClassList(
            MultiDexIO.readDexFile(
                true, file, Patcher.dexFileNamer, null, null
            ).also { opcodes = it.opcodes }.classes
        )

        /**
         * Write [classes] to [DexFile]s.
         *
         * @return The [DexFile]s.
         */
        internal fun writeDexFiles(): List<DexFile> {
            // Make sure to replace all classes with their proxy.
            val classes = classes.also(ProxyBackedClassList::applyProxies)
            val opcodes = opcodes

            // Create patched dex files.
            return mutableMapOf<String, MemoryDataStore>().also {
                val newDexFile = object : org.jf.dexlib2.iface.DexFile {
                    override fun getClasses() = classes.toSet()
                    override fun getOpcodes() = opcodes
                }

                // Write modified dex files.
                MultiDexIO.writeDexFile(
                    true, -1, // Core count.
                    it, Patcher.dexFileNamer, newDexFile, DexIO.DEFAULT_MAX_DEX_POOL_SIZE, null
                )
            }.map {
                DexFile(it.key, it.value.readAt(0))
            }
        }
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

            val resourceTable = try {
                androlib.getResTable(extInputFile, this !is Split.Library)
            } catch (exception: AndrolibException) {
                throw ApkException.Decode("Failed to get the resource table", exception)
            }

            when (mode) {
                ResourceDecodingMode.FULL -> {
                    val outDir = getResourceDirectory(options).also { it.mkdirs() }

                    try {
                        readResources(androlib, extInputFile, outDir, resourceTable)
                    } catch (exception: AndrolibException) {
                        throw ApkException.Decode("Failed to decode resources for $this", exception)
                    }

                    // Read additional metadata from the resource table.
                    with(packageMetadata) {
                        metaInfo.usesFramework = UsesFramework().also { framework ->
                            framework.ids = resourceTable.listFramePackages().map { it.id }.sorted()
                        }

                        // Read files to not compress.
                        metaInfo.doNotCompress = buildList {
                            androlib.recordUncompressedFiles(extInputFile, this)
                        }.takeIf { it.isNotEmpty() } // Uncomment this line to know why it is required.
                    }
                }

                ResourceDecodingMode.MANIFEST_ONLY -> {
                    // Create decoder for the resource table.
                    val decoder = ResAttrDecoder()
                    decoder.currentPackage = ResPackage(resourceTable, 0, null)

                    // Create xml parser with the decoder.
                    val aXmlParser = AXmlResourceParser()
                    aXmlParser.attrDecoder = decoder

                    // Parse package information with the decoder and parser which will set required values in the resource table
                    // and instead of decodeManifest, another more low level solution can be created to make it faster/better.
                    with(
                        XmlPullStreamDecoder(
                            aXmlParser, AndrolibResources().resXmlSerializer
                        )
                    ) {
                        try {
                            decodeManifest(
                                extInputFile.directory.getFileInput("AndroidManifest.xml"), nullOutputStream
                            )
                        } catch (exception: AndrolibException) {
                            throw ApkException.Decode("Failed to decode the manifest file for $this", exception)
                        }
                    }
                }
            }

            // Read of the resourceTable which is created by reading the manifest file.
            with(packageMetadata) {
                resourceTable.currentResPackage.name?.let { packageName = it }
                resourceTable.versionInfo.versionName?.let { packageVersion = it }

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
     * Write resources for a [Apk].
     *
     * @param options The [PatcherOptions] to write the resources with.
     */
    internal fun writeResources(options: PatcherOptions) {
        val apkWorkDirectory = getResourceDirectory(options).also {
            if (!it.exists()) throw ApkException.Write.ResourceDirectoryNotFound
        }

        // the resulting resource file
        val patchApk = options.patchDirectory.also { it.mkdirs() }.resolve(file.name).also { resources = it }

        val packageMetadata = packageMetadata
        val metaInfo = packageMetadata.metaInfo

        with(AndrolibResources().also { resources ->
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
        }) {
            writeResources(this, patchApk, apkWorkDirectory, metaInfo)
        }
    }

    /**
     * Write the resources for [Apk.file].
     *
     * @param resources The [AndrolibResources] to read the framework ids from.
     * @param patchApk The [Apk] file to write the resources to.
     * @param apkWorkDirectory The directory where the resources are stored.
     * @param metaInfo The [MetaInfo] for the [Apk] file.
     */
    protected open fun writeResources(
        resources: AndrolibResources, patchApk: File, apkWorkDirectory: File, metaInfo: MetaInfo
    ) = resources.aaptPackage(
        patchApk,
        apkWorkDirectory.resolve("AndroidManifest.xml")
            .also { ResXmlPatcher.fixingPublicAttrsInProviderAttributes(it) },
        apkWorkDirectory.resolve("res").takeUnless { this is Split.Library },
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

        /**
         * The prefix for resource names used by [Androlib], if it can not decode resources.
         */
        private const val DUMMY_RESOURCE_NAME_PREFIX = "APKTOOL_DUMMY_"
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
        val doNotCompress: Collection<String>?
            get() = metaInfo.doNotCompress

        /**
         * The package name of the [Apk] file.
         */
        var packageName: String = "unnamed split apk file"
            internal set

        /**
         * The package version of the [Apk] file.
         */
        var packageVersion: String = "0.0.0"
            internal set
    }

    /**
     * An exception thrown in [decodeResources] or [writeResources].
     *
     * @param message The exception message.
     * @param throwable The corresponding [Throwable].
     */
    sealed class ApkException(message: String, throwable: Throwable? = null) : Exception(message, throwable) {
        /**
         * An exception when decoding resources.
         *
         * @param message The exception message.
         * @param throwable The corresponding [Throwable].
         */
        class Decode(message: String, throwable: Throwable? = null) : ApkException(message, throwable)

        /**
         * An exception when writing resources.
         *
         * @param message The exception message.
         * @param throwable The corresponding [Throwable].
         */
        open class Write(message: String, throwable: Throwable? = null) : ApkException(message, throwable) {
            /**
             * An exception when a resource directory could not be found while writing.
             **/
            object ResourceDirectoryNotFound : Write("Failed to find the resource directory")
        }
    }
}