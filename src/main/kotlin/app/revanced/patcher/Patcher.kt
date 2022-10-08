package app.revanced.patcher

import app.revanced.patcher.data.Context
import app.revanced.patcher.data.findIndexed
import app.revanced.patcher.extensions.PatchExtensions.dependencies
import app.revanced.patcher.extensions.PatchExtensions.deprecated
import app.revanced.patcher.extensions.PatchExtensions.patchName
import app.revanced.patcher.extensions.nullOutputStream
import app.revanced.patcher.fingerprint.method.impl.MethodFingerprint.Companion.resolve
import app.revanced.patcher.patch.*
import app.revanced.patcher.util.VersionReader
import brut.androlib.Androlib
import brut.androlib.ApkDecoder
import brut.androlib.meta.UsesFramework
import brut.androlib.options.BuildOptions
import brut.androlib.res.AndrolibResources
import brut.androlib.res.data.ResPackage
import brut.androlib.res.decoder.AXmlResourceParser
import brut.androlib.res.decoder.ResAttrDecoder
import brut.androlib.res.decoder.XmlPullStreamDecoder
import brut.androlib.res.xml.ResXmlPatcher
import brut.directory.ExtFile
import brut.directory.ZipUtils
import lanchon.multidexlib2.BasicDexFileNamer
import lanchon.multidexlib2.DexIO
import lanchon.multidexlib2.MultiDexIO
import org.jf.dexlib2.Opcodes
import org.jf.dexlib2.iface.DexFile
import org.jf.dexlib2.writer.io.MemoryDataStore
import java.io.File
import java.io.FileOutputStream
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

private val NAMER = BasicDexFileNamer()

/**
 * The ReVanced Patcher.
 * @param options The options for the patcher.
 */
class Patcher(private val options: PatcherOptions) {
    private val base: Apk.Base
    private val splits: List<Apk.Split>

    val data: PatcherData

    private val logger = options.logger
    private val opcodes: Opcodes

    private var resourceDecodingMode = ResourceDecodingMode.MANIFEST_ONLY
    val context: PatcherContext

    companion object {
        @JvmStatic
        val version = VersionReader.read()
        private fun BuildOptions.setBuildOptions(options: PatcherOptions) {
            this.aaptPath = options.aaptPath
            this.useAapt2 = true
            this.frameworkFolderLocation = options.frameworkPath
        }
    }

    init {
        logger.info("Reading dex files")

        // prepare files
        options.inputFiles.let { files ->
            // set the base apk for the patcher
            base = files.find { it is Apk.Base } as Apk.Base

            // set the splits for the patcher
            splits = files.filterIsInstance<Apk.Split>()
        }
        // read dex files from the base apk
        val dexFile = MultiDexIO.readDexFile(true, base.file, NAMER, null, null)
        // get the opcodes
        opcodes = dexFile.opcodes
        // finally create patcher context
        context = PatcherContext(dexFile.classes.toMutableList(), File(options.resourceCacheDirectory))
        // decode manifest file
        decodeResources(ResourceDecodingMode.MANIFEST_ONLY)
    }

    /**
     * Add [Patch]es to the patcher.
     * @param patches [Patch]es The patches to add.
     */
    fun addPatches(patches: Iterable<Class<out Patch<Context>>>) {
        /**
         * Fill the cache with the instances of the [Patch]es for later use.
         * Note: Dependencies of the [Patch] will be cached as well.
         */
        fun Class<out Patch<Context>>.isResource() {
            this.also {
                if (!ResourcePatch::class.java.isAssignableFrom(it)) return@also
                // set the mode to decode all resources before running the patches
                resourceDecodingMode = ResourceDecodingMode.FULL
            }.dependencies?.forEach { it.java.isResource() }
        }

        context.patches.addAll(patches.onEach(Class<out Patch<Context>>::isResource))
    }

    /**
     * Add additional dex file container to the patcher.
     * @param files The dex file containers to add to the patcher.
     * @param allowedOverwrites A list of class types that are allowed to be overwritten.
     * @param throwOnDuplicates If this is set to true, the patcher will throw an exception if a duplicate class has been found.
     */
    fun addFiles(
        files: List<File>,
        allowedOverwrites: Iterable<String> = emptyList(),
        throwOnDuplicates: Boolean = false,
        callback: (File) -> Unit
    ) {
        for (file in files) {
            var modified = false
            for (classDef in MultiDexIO.readDexFile(true, file, NAMER, null, null).classes) {
                val type = classDef.type

                val existingClass = context.bytecodeContext.classes.classes.findIndexed { it.type == type }
                if (existingClass == null) {
                    if (throwOnDuplicates) throw Exception("Class $type has already been added to the patcher")

                    logger.trace("Merging $type")
                    context.bytecodeContext.classes.classes.add(classDef)
                    modified = true

                    continue
                }

                if (!allowedOverwrites.contains(type)) continue

                logger.trace("Overwriting $type")

                val index = existingClass.second
                context.bytecodeContext.classes.classes[index] = classDef
                modified = true
            }
            if (modified) callback(file)
        }
    }

    /**
     * Decode resources for the patcher.
     *
     * @param mode The [ResourceDecodingMode] to use when decoding.
     */
    private fun decodeResources(mode: ResourceDecodingMode) {
        /**
         * Decode resources for a [Apk].
         * Note: This function respects the patchers [ResourceDecodingMode].
         *
         * @param apk The [Apk] to decode resources for.
         */
        fun decodeResources(apk: Apk) {
            val extInputFile = ExtFile(apk.file)
            try {
                val androlib = Androlib(BuildOptions().also { it.setBuildOptions(options) })
                val resourceTable = androlib.getResTable(extInputFile, true)
                when (mode) {
                    ResourceDecodingMode.FULL -> {
                        val outDir = File(options.resourceCacheDirectory).resolve(apk.toString())
                        outDir.mkdirs()

                        // always decode the manifest file
                        androlib.decodeManifestWithResources(extInputFile, outDir, resourceTable)

                        when (apk) {
                            is Apk.Split.Library -> {
                                // only unpack raw files, such as libs
                                androlib.decodeRawFiles(extInputFile, outDir, ApkDecoder.DECODE_ASSETS_NONE)
                            }
                            else -> {
                                // decode resources to cache directory
                                androlib.decodeResourcesFull(extInputFile, outDir, resourceTable)
                            }
                        }

                        // read additional metadata from the resource table
                        apk.packageMetadata.let { metadata ->
                            metadata.metaInfo.usesFramework = UsesFramework().also { framework ->
                                framework.ids = resourceTable.listFramePackages().map { it.id }.sorted()
                            }

                            // read files to not compress
                            metadata.metaInfo.doNotCompress = buildList {
                                androlib.recordUncompressedFiles(extInputFile, this)
                            }
                        }

                    }
                    ResourceDecodingMode.MANIFEST_ONLY -> {
                        logger.info("Decoding AndroidManifest.xml only, because resources are not needed")

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
                apk.packageMetadata.let { metadata ->
                    metadata.packageName = resourceTable.currentResPackage.name
                    metadata.packageVersion = resourceTable.versionInfo.versionName
                    metadata.metaInfo.versionInfo = resourceTable.versionInfo
                    metadata.metaInfo.sdkInfo = resourceTable.sdkInfo
                }
            } finally {
                extInputFile.close()
            }
        }

        // if the mode is full, all apk's resources are required to be decoded
        // the manifest of the base is previously decoded already
        if (mode != ResourceDecodingMode.FULL) return
        logger.info("Decoding resources")
        options.inputFiles.forEach(::decodeResources)
    }

    /**
     * Execute patches added the patcher.
     *
     * @param stopOnError If true, the patches will stop on the first error.
     * @return A pair of the name of the [Patch] and its [PatchResult].
     */
    fun executePatches(stopOnError: Boolean = false): Sequence<Pair<String, Result<PatchResultSuccess>>> {
        /**
         * Execute a [Patch] and its dependencies recursively.
         *
         * @param patchClass The [Patch] to execute.
         * @param executedPatches A map of [Patch]es paired to a boolean indicating their success, to prevent infinite recursion.
         * @return The result of executing the [Patch].
         */
        fun executePatch(
            patchClass: Class<out Patch<Context>>,
            executedPatches: LinkedHashMap<String, ExecutedPatch>
        ): PatchResult {
            val patchName = patchClass.patchName

            // if the patch has already applied silently skip it
            if (executedPatches.contains(patchName)) {
                if (!executedPatches[patchName]!!.success)
                    return PatchResultError("'$patchName' did not succeed previously")

                logger.trace("Skipping '$patchName' because it has already been applied")

                return PatchResultSuccess()
            }

            // recursively execute all dependency patches
            patchClass.dependencies?.forEach { dependencyClass ->
                val dependency = dependencyClass.java

                val result = executePatch(dependency, executedPatches)
                if (result.isSuccess()) return@forEach

                val error = result.error()!!
                val errorMessage = error.cause ?: error.message
                return PatchResultError("'$patchName' depends on '${dependency.patchName}' but the following error was raised: $errorMessage")
            }

            patchClass.deprecated?.let { (reason, replacement) ->
                logger.warn("'$patchName' is deprecated, reason: $reason")
                if (replacement != null) logger.warn("Use '${replacement.java.patchName}' instead")
            }

            val isResourcePatch = ResourcePatch::class.java.isAssignableFrom(patchClass)
            val patchInstance = patchClass.getDeclaredConstructor().newInstance()

            // TODO: implement this in a more polymorphic way
            val patchContext = if (isResourcePatch) {
                context.resourceContext
            } else {
                context.bytecodeContext.also { context ->
                    (patchInstance as BytecodePatch).fingerprints?.resolve(
                        context,
                        context.classes.classes
                    )
                }
            }

            logger.trace("Executing '$patchName' of type: ${if (isResourcePatch) "resource" else "bytecode"}")

            return try {
                patchInstance.execute(patchContext).also {
                    executedPatches[patchName] = ExecutedPatch(patchInstance, it.isSuccess())
                }
            } catch (e: Exception) {
                PatchResultError(e).also {
                    executedPatches[patchName] = ExecutedPatch(patchInstance, false)
                }
            }
        }

        // prevent from decoding the manifest twice if it is not needed
        if (resourceDecodingMode == ResourceDecodingMode.FULL) decodeResources(ResourceDecodingMode.FULL)

            logger.trace("Executing all patches")

            val executedPatches = LinkedHashMap<String, ExecutedPatch>() // first is name

            try {
                context.patches.forEach { patch ->
                    val patchResult = executePatch(patch, executedPatches)

                    val result = if (patchResult.isSuccess()) {
                        Result.success(patchResult.success()!!)
                    } else {
                        Result.failure(patchResult.error()!!)
                    }

                    yield(patch.patchName to result)
                    if (stopOnError && patchResult.isError()) return@sequence
                }
            } finally {
                executedPatches.values.reversed().forEach { (patch, _) ->
                    patch.close()
                }
            }
        }
    }

    /**
     * Save the patched dex file.
     */
    fun save(): PatcherResult {
        fun compileResources(apk: Apk) {
            val packageMetadata = apk.packageMetadata
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
            val apkCacheDirectory = cacheDirectory.resolve(apk.toString())

            // the resulting resource file
            val patchApk = cacheDirectory.resolve(apk.file.nameWithoutExtension + "_patch.apk")
                .also { apk.resources = it }

            when (apk) {
                is Apk.Base,
                is Apk.Split.Asset,
                is Apk.Split.Language -> {
                    // package resource files
                    androlibResources.aaptPackage(
                        patchApk,
                        apkCacheDirectory.resolve("AndroidManifest.xml")
                            .also { ResXmlPatcher.fixingPublicAttrsInProviderAttributes(it) },
                        apkCacheDirectory.resolve("res"),
                        null,
                        null,
                        metaInfo.usesFramework.ids.map { id ->
                            androlibResources.getFrameworkApk(
                                id, metaInfo.usesFramework.tag
                            )
                        }.toTypedArray()
                    )
                }
                is Apk.Split.Library -> {
                    // do not compress libraries for speed, because the patchApk is a temporal file
                    val doNotCompress = apkCacheDirectory.listFiles()?.map { it.name }
                    ZipUtils.zipFolders(apkCacheDirectory, patchApk, null, doNotCompress)
                }
            }
        }

        when (resourceDecodingMode) {
            ResourceDecodingMode.FULL -> {
                logger.info("Compiling resources")
                options.inputFiles.forEach(::compileResources)
            }
            else -> logger.info("Not compiling resources because resource patching is not required")
        }

        // create patched dex files
        val dexFiles = mutableMapOf<String, MemoryDataStore>().also {
            logger.trace("Creating new dex file")
            val newDexFile = object : DexFile {
                override fun getClasses() = context.bytecodeContext.classes.also { it.replaceClasses() }
                override fun getOpcodes() = this@Patcher.opcodes
            }

            // write modified dex files
            logger.info("Writing modified dex files")
            MultiDexIO.writeDexFile(
                true, -1, // core count
                it, NAMER, newDexFile, DexIO.DEFAULT_MAX_DEX_POOL_SIZE, null
            )
        }.map {
            app.revanced.patcher.util.dex.DexFile(it.key, it.value.readAt(0))
        }

        // save the patch to the base apk
        base.dexFiles = dexFiles

        // collect the patched files
        val patchedFiles = mutableListOf<Apk>().also { it.addAll(splits) }.also { it.add(base) }

        return PatcherResult(patchedFiles)
    }

    /**
     * The type of decoding the resources.
     */
    private enum class ResourceDecodingMode {
        /**
         * Decode all resources.
         */
        FULL,

        /**
         * Decode the manifest file only.
         */
        MANIFEST_ONLY,
    }
}

/**
 * A result of executing a [Patch].
 *
 * @param patchInstance The instance of the [Patch] that was applied.
 * @param success The result of the [Patch].
 */
internal data class ExecutedPatch(val patchInstance: Patch<Context>, val success: Boolean)