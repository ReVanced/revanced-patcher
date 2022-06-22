package app.revanced.patcher

import app.revanced.patcher.data.PackageMetadata
import app.revanced.patcher.data.PatcherData
import app.revanced.patcher.data.base.Data
import app.revanced.patcher.data.implementation.findIndexed
import app.revanced.patcher.extensions.PatchExtensions.dependencies
import app.revanced.patcher.extensions.PatchExtensions.patchName
import app.revanced.patcher.extensions.nullOutputStream
import app.revanced.patcher.patch.base.Patch
import app.revanced.patcher.patch.implementation.BytecodePatch
import app.revanced.patcher.patch.implementation.ResourcePatch
import app.revanced.patcher.patch.implementation.misc.PatchResult
import app.revanced.patcher.patch.implementation.misc.PatchResultError
import app.revanced.patcher.patch.implementation.misc.PatchResultSuccess
import app.revanced.patcher.signature.implementation.method.resolver.MethodSignatureResolver
import app.revanced.patcher.util.ListBackedSet
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
import java.io.File
import java.nio.file.Files

val NAMER = BasicDexFileNamer()

/**
 * The ReVanced Patcher.
 * @param options The options for the patcher.
 */
class Patcher(private val options: PatcherOptions) {
    val data: PatcherData
    private val opcodes: Opcodes

    init {
        val extInputFile = ExtFile(options.inputFile)
        val outDir = File(options.resourceCacheDirectory)
        if (outDir.exists()) outDir.deleteRecursively()
        outDir.mkdirs()

        val androlib = Androlib(BuildOptions().also { it.setBuildOptions(options) })
        val resourceTable = androlib.getResTable(extInputFile, true)

        val packageMetadata = PackageMetadata()

        if (options.patchResources) {
            // decode resources to cache directory
            androlib.decodeManifestWithResources(extInputFile, outDir, resourceTable)
            androlib.decodeResourcesFull(extInputFile, outDir, resourceTable)

            // read additional metadata from the resource table
            packageMetadata.metaInfo.usesFramework = UsesFramework().also { framework ->
                framework.ids = resourceTable.listFramePackages().map { it.id }.sorted()
            }

            packageMetadata.metaInfo.doNotCompress = buildList {
                androlib.recordUncompressedFiles(extInputFile, this)
            }

        } else {
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

        packageMetadata.packageName = resourceTable.currentResPackage.name
        packageMetadata.packageVersion = resourceTable.versionInfo.versionName
        packageMetadata.metaInfo.versionInfo = resourceTable.versionInfo
        packageMetadata.metaInfo.sdkInfo = resourceTable.sdkInfo

        // read dex files
        val dexFile = MultiDexIO.readDexFile(true, options.inputFile, NAMER, null, null)
        // get the opcodes
        opcodes = dexFile.opcodes

        // finally create patcher data
        data = PatcherData(
            dexFile.classes.toMutableList(), options.resourceCacheDirectory, packageMetadata
        )
    }

    /**
     * Add additional dex file container to the patcher.
     * @param files The dex file containers to add to the patcher.
     * @param allowedOverwrites A list of class types that are allowed to be overwritten.
     * @param throwOnDuplicates If this is set to true, the patcher will throw an exception if a duplicate class has been found.
     */
    fun addFiles(
        files: List<File>, allowedOverwrites: Iterable<String> = emptyList(), throwOnDuplicates: Boolean = false
    ) {
        for (file in files) {
            for (classDef in MultiDexIO.readDexFile(true, file, NAMER, null, null).classes) {
                val e = data.bytecodeData.classes.internalClasses.findIndexed { it.type == classDef.type }
                if (e != null) {
                    if (throwOnDuplicates) {
                        throw Exception("Class ${classDef.type} has already been added to the patcher.")
                    }
                    val (_, idx) = e
                    if (allowedOverwrites.contains(classDef.type)) {
                        data.bytecodeData.classes.internalClasses[idx] = classDef
                    }
                    continue
                }
                data.bytecodeData.classes.internalClasses.add(classDef)
            }
        }
    }

    /**
     * Save the patched dex file.
     */
    fun save(): PatcherResult {
        val packageMetadata = data.packageMetadata
        val metaInfo = packageMetadata.metaInfo
        var resourceFile: File? = null

        if (options.patchResources) {
            val cacheDirectory = ExtFile(options.resourceCacheDirectory)

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
                    id,
                    metaInfo.usesFramework.tag
                )
            }.toTypedArray()

            androlibResources.aaptPackage(
                aaptFile, manifestFile, resDirectory, null,
                null, includedFiles
            )

            resourceFile = aaptFile
        }

        val newDexFile = object : DexFile {
            override fun getClasses(): Set<ClassDef> {
                data.bytecodeData.classes.applyProxies()
                return ListBackedSet(data.bytecodeData.classes.internalClasses)
            }

            override fun getOpcodes(): Opcodes {
                return this@Patcher.opcodes
            }
        }

        // write dex modified files
        val dexFiles = mutableMapOf<String, MemoryDataStore>()
        MultiDexIO.writeDexFile(
            true, -1, // core count
            dexFiles, NAMER, newDexFile, DexIO.DEFAULT_MAX_DEX_POOL_SIZE, null
        )

        return PatcherResult(
            dexFiles.map {
                app.revanced.patcher.util.dex.DexFile(it.key, it.value)
            },
            metaInfo.doNotCompress.toList(),
            resourceFile
        )
    }

    /**
     * Add [Patch]es to the patcher.
     * @param patches [Patch]es The patches to add.
     */
    fun addPatches(patches: Iterable<Class<out Patch<Data>>>) {
        data.patches.addAll(patches)
    }

    /**
     * Apply a [patch] and its dependencies recursively.
     * @param patch The [patch] to apply.
     * @param appliedPatches A list of [patch] names, to prevent applying [patch]es twice.
     * @return The result of executing the [patch].
     */
    private fun applyPatch(
        patch: Class<out Patch<Data>>, appliedPatches: MutableList<String>
    ): PatchResult {
        val patchName = patch.patchName

        // if the patch has already applied silently skip it
        if (appliedPatches.contains(patchName)) return PatchResultSuccess()
        appliedPatches.add(patchName)

        // recursively apply all dependency patches
        patch.dependencies?.forEach {
            val patchDependency = it.java

            val result = applyPatch(patchDependency, appliedPatches)
            if (result.isSuccess()) return@forEach

            val errorMessage = result.error()!!.message
            return PatchResultError("$patchName depends on ${patchDependency.patchName} but the following error was raised: $errorMessage")
        }

        val patchInstance = patch.getDeclaredConstructor().newInstance()

        // if the current patch is a resource patch but resource patching is disabled, return an error
        val isResourcePatch = patchInstance is ResourcePatch
        if (!options.patchResources && isResourcePatch) return PatchResultError("$patchName is a resource patch, but resource patching is disabled.")

        // TODO: find a solution for this
        val data = if (isResourcePatch) {
            data.resourceData
        } else {
            MethodSignatureResolver(
                data.bytecodeData.classes.internalClasses, (patchInstance as BytecodePatch).signatures
            ).resolve(data)
            data.bytecodeData
        }

        return try {
            patchInstance.execute(data)
        } catch (e: Exception) {
            PatchResultError(e)
        }
    }

    /**
     * Apply patches loaded into the patcher.
     * @param stopOnError If true, the patches will stop on the first error.
     * @return A map of [PatchResultSuccess]. If the [Patch] was successfully applied,
     * [PatchResultSuccess] will always be returned to the wrapping Result object.
     * If the [Patch] failed to apply, an Exception will always be returned to the wrapping Result object.
     */
    fun applyPatches(
        stopOnError: Boolean = false, callback: (String) -> Unit = {}
    ): Map<String, Result<PatchResultSuccess>> {
        val appliedPatches = mutableListOf<String>()

        return buildMap {
            for (patch in data.patches) {
                val result = applyPatch(patch, appliedPatches)

                val name = patch.patchName
                callback(name)

                this[name] = if (result.isSuccess()) {
                    Result.success(result.success()!!)
                } else {
                    Result.failure(result.error()!!)
                }

                if (stopOnError && result.isError()) break
            }
        }
    }
}

private fun BuildOptions.setBuildOptions(options: PatcherOptions) {
    this.aaptPath = options.aaptPath
    this.useAapt2 = true
    this.frameworkFolderLocation = options.frameworkFolderLocation
}