package app.revanced.patcher

import app.revanced.patcher.data.Data
import app.revanced.patcher.data.impl.findIndexed
import app.revanced.patcher.extensions.PatchExtensions.dependencies
import app.revanced.patcher.extensions.PatchExtensions.deprecated
import app.revanced.patcher.extensions.PatchExtensions.patchName
import app.revanced.patcher.extensions.nullOutputStream
import app.revanced.patcher.fingerprint.method.impl.MethodFingerprint.Companion.resolve
import app.revanced.patcher.patch.Patch
import app.revanced.patcher.patch.PatchResult
import app.revanced.patcher.patch.PatchResultError
import app.revanced.patcher.patch.PatchResultSuccess
import app.revanced.patcher.patch.impl.BytecodePatch
import app.revanced.patcher.patch.impl.ResourcePatch
import app.revanced.patcher.util.ListBackedSet
import app.revanced.patcher.util.VersionReader
import brut.androlib.Androlib
import brut.androlib.meta.UsesFramework
import brut.androlib.options.BuildOptions
import brut.androlib.res.AndrolibResources
import brut.androlib.res.data.ResPackage
import brut.androlib.res.decoder.AXmlResourceParser
import brut.androlib.res.decoder.ResAttrDecoder
import brut.androlib.res.decoder.XmlPullStreamDecoder
import brut.androlib.res.xml.ResXmlPatcher
import brut.directory.ExtFile
import lanchon.multidexlib2.BasicDexFileNamer
import lanchon.multidexlib2.DexIO
import lanchon.multidexlib2.MultiDexIO
import org.jf.dexlib2.Opcodes
import org.jf.dexlib2.iface.ClassDef
import org.jf.dexlib2.iface.DexFile
import org.jf.dexlib2.writer.io.MemoryDataStore
import java.io.Closeable
import java.io.File
import java.nio.file.Files

private val NAMER = BasicDexFileNamer()

/**
 * The ReVanced Patcher.
 * @param options The options for the patcher.
 */
class Patcher(private val options: PatcherOptions) {
    private val logger = options.logger
    private val opcodes: Opcodes
    private var resourceDecodingMode = ResourceDecodingMode.MANIFEST_ONLY
    val data: PatcherData

    companion object {
        @JvmStatic
        val version = VersionReader.read()
        private fun BuildOptions.setBuildOptions(options: PatcherOptions) {
            this.aaptPath = options.aaptPath
            this.useAapt2 = true
            this.frameworkFolderLocation = options.frameworkFolderLocation
        }
    }

    init {
        logger.info("Reading dex files")
        // read dex files
        val dexFile = MultiDexIO.readDexFile(true, options.inputFile, NAMER, null, null)
        // get the opcodes
        opcodes = dexFile.opcodes
        // finally create patcher data
        data = PatcherData(dexFile.classes.toMutableList(), options.resourceCacheDirectory)

        // decode manifest file
        decodeResources(ResourceDecodingMode.MANIFEST_ONLY)
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

                val existingClass = data.bytecodeData.classes.internalClasses.findIndexed { it.type == type }
                if (existingClass == null) {
                    if (throwOnDuplicates) throw Exception("Class $type has already been added to the patcher")

                    logger.trace("Merging $type")
                    data.bytecodeData.classes.internalClasses.add(classDef)
                    modified = true

                    continue
                }

                if (!allowedOverwrites.contains(type)) continue

                logger.trace("Overwriting $type")

                val index = existingClass.second
                data.bytecodeData.classes.internalClasses[index] = classDef
                modified = true
            }
            if (modified) callback(file)
        }
    }

    /**
     * Save the patched dex file.
     */
    fun save(): PatcherResult {
        val packageMetadata = data.packageMetadata
        val metaInfo = packageMetadata.metaInfo
        var resourceFile: File? = null

        when (resourceDecodingMode) {
            ResourceDecodingMode.FULL -> {
                val cacheDirectory = ExtFile(options.resourceCacheDirectory)
                try {
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

                    val manifestFile = cacheDirectory.resolve("AndroidManifest.xml")

                    ResXmlPatcher.fixingPublicAttrsInProviderAttributes(manifestFile)

                    val aaptFile = cacheDirectory.resolve("aapt_temp_file")

                    // delete if it exists
                    Files.deleteIfExists(aaptFile.toPath())

                    val resDirectory = cacheDirectory.resolve("res")
                    val includedFiles = metaInfo.usesFramework.ids.map { id ->
                        androlibResources.getFrameworkApk(
                            id, metaInfo.usesFramework.tag
                        )
                    }.toTypedArray()

                    logger.info("Compiling resources")
                    androlibResources.aaptPackage(
                        aaptFile, manifestFile, resDirectory, null, null, includedFiles
                    )

                    resourceFile = aaptFile
                } finally {
                    cacheDirectory.close()
                }
            }
            else -> logger.info("Not compiling resources because resource patching is not required")
        }

        logger.trace("Creating new dex file")
        val newDexFile = object : DexFile {
            override fun getClasses(): Set<ClassDef> {
                data.bytecodeData.classes.applyProxies()
                return ListBackedSet(data.bytecodeData.classes.internalClasses)
            }

            override fun getOpcodes(): Opcodes {
                return this@Patcher.opcodes
            }
        }

        // write modified dex files
        logger.info("Writing modified dex files")
        val dexFiles = mutableMapOf<String, MemoryDataStore>()
        MultiDexIO.writeDexFile(
            true, -1, // core count
            dexFiles, NAMER, newDexFile, DexIO.DEFAULT_MAX_DEX_POOL_SIZE, null
        )

        return PatcherResult(
            dexFiles.map {
                app.revanced.patcher.util.dex.DexFile(it.key, it.value.readAt(0))
            },
            metaInfo.doNotCompress?.toList(),
            resourceFile
        )
    }

    /**
     * Add [Patch]es to the patcher.
     * @param patches [Patch]es The patches to add.
     */
    fun addPatches(patches: Iterable<Class<out Patch<Data>>>) {
        /**
         * Fill the cache with the instances of the [Patch]es for later use.
         * Note: Dependencies of the [Patch] will be cached as well.
         */
        fun Class<out Patch<Data>>.isResource() {
            this.also {
                if (!ResourcePatch::class.java.isAssignableFrom(it)) return@also
                // set the mode to decode all resources before running the patches
                resourceDecodingMode = ResourceDecodingMode.FULL
            }.dependencies?.forEach { it.java.isResource() }
        }

        data.patches.addAll(patches.onEach(Class<out Patch<Data>>::isResource))
    }

    /**
     * Apply a [patch] and its dependencies recursively.
     * @param patch The [patch] to apply.
     * @param appliedPatches A map of [patch]es paired to a boolean indicating their success, to prevent infinite recursion.
     * @return The result of executing the [patch].
     */
    private fun applyPatch(
        patch: Class<out Patch<Data>>,
        appliedPatches: LinkedHashMap<String, AppliedPatch>
    ): PatchResult {
        val patchName = patch.patchName

        // if the patch has already applied silently skip it
        if (appliedPatches.contains(patchName)) {
            if (!appliedPatches[patchName]!!.success)
                return PatchResultError("'$patchName' did not succeed previously")

            logger.trace("Skipping '$patchName' because it has already been applied")

            return PatchResultSuccess()
        }

        // recursively apply all dependency patches
        patch.dependencies?.forEach { dependencyClass ->
            val dependency = dependencyClass.java

            val result = applyPatch(dependency, appliedPatches)
            if (result.isSuccess()) return@forEach

            val error = result.error()!!
            val errorMessage = error.cause ?: error.message
            return PatchResultError("'$patchName' depends on '${dependency.patchName}' but the following error was raised: $errorMessage")
        }

        patch.deprecated?.let { (reason, replacement) ->
            logger.warn("'$patchName' is deprecated, reason: $reason")
            if (replacement != null) logger.warn("Use '${replacement.java.patchName}' instead")
        }

        val patchInstance = patch.getDeclaredConstructor().newInstance()

        val isResourcePatch = ResourcePatch::class.java.isAssignableFrom(patch)
        // TODO: implement this in a more polymorphic way
        val data = if (isResourcePatch) {
            data.resourceData
        } else {
            data.bytecodeData.also { data ->
                (patchInstance as BytecodePatch).fingerprints?.resolve(
                    data,
                    data.classes.internalClasses
                )
            }
        }

        logger.trace("Executing '$patchName' of type: ${if (isResourcePatch) "resource" else "bytecode"}")

        return try {
            patchInstance.execute(data).also {
                appliedPatches[patchName] = AppliedPatch(patchInstance, it.isSuccess())
            }
        } catch (e: Exception) {
            PatchResultError(e).also {
                appliedPatches[patchName] = AppliedPatch(patchInstance, false)
            }
        }
    }

    /**
     * Decode resources for the patcher.
     *
     * @param mode The [ResourceDecodingMode] to use when decoding.
     */
    private fun decodeResources(mode: ResourceDecodingMode) {
        val extInputFile = ExtFile(options.inputFile)
        try {
            val androlib = Androlib(BuildOptions().also { it.setBuildOptions(options) })
            val resourceTable = androlib.getResTable(extInputFile, true)
            when (mode) {
                ResourceDecodingMode.FULL -> {
                    val outDir = File(options.resourceCacheDirectory)
                    if (outDir.exists()) {
                        logger.info("Deleting existing resource cache directory")
                        if (!outDir.deleteRecursively()) {
                            logger.error("Failed to delete existing resource cache directory")
                        }
                    }
                    outDir.mkdirs()

                    logger.info("Decoding resources")

                    // decode resources to cache directory
                    androlib.decodeManifestWithResources(extInputFile, outDir, resourceTable)
                    androlib.decodeResourcesFull(extInputFile, outDir, resourceTable)

                    // read additional metadata from the resource table
                    data.packageMetadata.let { metadata ->
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
            data.packageMetadata.let { metadata ->
                metadata.packageName = resourceTable.currentResPackage.name
                metadata.packageVersion = resourceTable.versionInfo.versionName
                metadata.metaInfo.versionInfo = resourceTable.versionInfo
                metadata.metaInfo.sdkInfo = resourceTable.sdkInfo
            }
        } finally {
            extInputFile.close()
        }
    }

    /**
     * Apply patches loaded into the patcher.
     * @param stopOnError If true, the patches will stop on the first error.
     * @return A pair of the name of the [Patch] and its [PatchResult].
     */
    fun applyPatches(stopOnError: Boolean = false) = sequence {
        // prevent from decoding the manifest twice if it is not needed
        if (resourceDecodingMode == ResourceDecodingMode.FULL) decodeResources(ResourceDecodingMode.FULL)

        logger.trace("Applying all patches")

        val appliedPatches = LinkedHashMap<String, AppliedPatch>() // first is name

        try {
            for (patch in data.patches) {
                val patchResult = applyPatch(patch, appliedPatches)

                val result = if (patchResult.isSuccess()) {
                    Result.success(patchResult.success()!!)
                } else {
                    Result.failure(patchResult.error()!!)
                }

                yield(patch.patchName to result)
                if (stopOnError && patchResult.isError()) break
            }
        } finally {
            // close all closeable patches in order
            for ((patch, _) in appliedPatches.values.reversed()) {
                if (patch !is Closeable) continue

                patch.close()
            }
        }
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
 * A result of applying a [Patch].
 *
 * @param patchInstance The instance of the [Patch] that was applied.
 * @param success The result of the [Patch].
 */
internal data class AppliedPatch(val patchInstance: Patch<Data>, val success: Boolean)