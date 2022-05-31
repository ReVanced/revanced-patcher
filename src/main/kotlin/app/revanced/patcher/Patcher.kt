package app.revanced.patcher

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
import brut.androlib.res.AndrolibResources
import brut.androlib.res.data.ResPackage
import brut.androlib.res.decoder.AXmlResourceParser
import brut.androlib.res.decoder.ResAttrDecoder
import brut.androlib.res.decoder.XmlPullStreamDecoder
import brut.directory.ExtFile
import lanchon.multidexlib2.BasicDexFileNamer
import lanchon.multidexlib2.DexIO
import lanchon.multidexlib2.MultiDexIO
import org.jf.dexlib2.Opcodes
import org.jf.dexlib2.iface.ClassDef
import org.jf.dexlib2.iface.DexFile
import org.jf.dexlib2.writer.io.MemoryDataStore
import java.io.File

val NAMER = BasicDexFileNamer()

/**
 * The ReVanced Patcher.
 * @param options The options for the patcher.
 */
class Patcher(
    private val options: PatcherOptions
) {
    val packageVersion: String
    val packageName: String

    private lateinit var usesFramework: UsesFramework
    private val patcherData: PatcherData
    private val opcodes: Opcodes

    init {
        val extFileInput = ExtFile(options.inputFile)
        val outDir = File(options.resourceCacheDirectory)

        if (outDir.exists()) outDir.deleteRecursively()
        outDir.mkdir()

        // load the resource table from the input file
        val androlib = Androlib()
        val resourceTable = androlib.getResTable(extFileInput, true)

        if (options.patchResources) {
            // 1. decode resources to cache directory
            androlib.decodeManifestWithResources(extFileInput, outDir, resourceTable)
            androlib.decodeResourcesFull(extFileInput, outDir, resourceTable)

            // 2. read framework ids from the resource table
            usesFramework = UsesFramework()
            usesFramework.ids = resourceTable.listFramePackages().map { it.id }.sorted()
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
                extFileInput.directory.getFileInput("AndroidManifest.xml"), nullOutputStream
            )
        }

        // set package information
        packageVersion = resourceTable.versionInfo.versionName
        packageName = resourceTable.currentResPackage.name
        // read dex files
        val dexFile = MultiDexIO.readDexFile(true, options.inputFile, NAMER, null, null)
        opcodes = dexFile.opcodes

        // save to patcher data
        patcherData = PatcherData(dexFile.classes.toMutableList(), options.resourceCacheDirectory)
    }

    /**
     * Add additional dex file container to the patcher.
     * @param files The dex file containers to add to the patcher.
     * @param allowedOverwrites A list of class types that are allowed to be overwritten.
     * @param throwOnDuplicates If this is set to true, the patcher will throw an exception if a duplicate class has been found.
     */
    fun addFiles(
        files: Iterable<File>, allowedOverwrites: Iterable<String> = emptyList(), throwOnDuplicates: Boolean = false
    ) {
        for (file in files) {
            val dexFile = MultiDexIO.readDexFile(true, file, NAMER, null, null)
            for (classDef in dexFile.classes) {
                val e = patcherData.bytecodeData.classes.internalClasses.findIndexed { it.type == classDef.type }
                if (e != null) {
                    if (throwOnDuplicates) {
                        throw Exception("Class ${classDef.type} has already been added to the patcher.")
                    }
                    val (_, idx) = e
                    if (allowedOverwrites.contains(classDef.type)) {
                        patcherData.bytecodeData.classes.internalClasses[idx] = classDef
                    }
                    continue
                }
                patcherData.bytecodeData.classes.internalClasses.add(classDef)
            }
        }
    }

    /**
     * Save the patched dex file.
     */
    fun save(): Map<String, MemoryDataStore> {
        val newDexFile = object : DexFile {
            override fun getClasses(): Set<ClassDef> {
                patcherData.bytecodeData.classes.applyProxies()
                return ListBackedSet(patcherData.bytecodeData.classes.internalClasses)
            }

            override fun getOpcodes(): Opcodes {
                return this@Patcher.opcodes
            }
        }

        // build modified resources
        if (options.patchResources) {
            val extDir = ExtFile(options.resourceCacheDirectory)

            // TODO: figure out why a new instance of Androlib is necessary here
            Androlib().buildResources(extDir, usesFramework)
        }

        // write dex modified files
        val output = mutableMapOf<String, MemoryDataStore>()
        MultiDexIO.writeDexFile(
            true, -1, // core count
            output, NAMER, newDexFile, DexIO.DEFAULT_MAX_DEX_POOL_SIZE, null
        )
        return output
    }

    /**
     * Add [Patch]es to the patcher.
     * @param patches [Patch]es The patches to add.
     */
    fun addPatches(patches: Iterable<Class<out Patch<Data>>>) {
        patcherData.patches.addAll(patches)
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
            patcherData.resourceData
        } else {
            MethodSignatureResolver(
                patcherData.bytecodeData.classes.internalClasses, (patchInstance as BytecodePatch).signatures
            ).resolve(patcherData)
            patcherData.bytecodeData
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
            for (patch in patcherData.patches) {
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
