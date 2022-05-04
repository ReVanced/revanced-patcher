package app.revanced.patcher

import app.revanced.patcher.data.PatcherData
import app.revanced.patcher.data.base.Data
import app.revanced.patcher.data.implementation.findIndexed
import app.revanced.patcher.patch.base.Patch
import app.revanced.patcher.patch.implementation.BytecodePatch
import app.revanced.patcher.patch.implementation.ResourcePatch
import app.revanced.patcher.patch.implementation.metadata.PatchMetadata
import app.revanced.patcher.patch.implementation.misc.PatchResultSuccess
import app.revanced.patcher.signature.MethodSignature
import app.revanced.patcher.signature.resolver.SignatureResolver
import app.revanced.patcher.util.ListBackedSet
import brut.androlib.Androlib
import brut.androlib.ApkDecoder
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
 * @param inputFile The input file (usually an apk file).
 * @param resourceCacheDirectory Directory to cache resources.
 * @param patchResources Weather to use the resource patcher. Resources will still need to be decoded.
 */
class Patcher(
    inputFile: File,
    // TODO: maybe a file system in memory is better. Could cause high memory usage.
    private val resourceCacheDirectory: String,
    private val patchResources: Boolean = false
) {
    val packageVersion: String
    val packageName: String

    private val patcherData: PatcherData
    private val opcodes: Opcodes
    private var signaturesResolved = false
    private val androlib = Androlib()

    init {
        // FIXME: only use androlib instead of ApkDecoder which is currently a temporal solution
        val decoder = ApkDecoder(androlib)

        decoder.setApkFile(inputFile)
        decoder.setDecodeSources(ApkDecoder.DECODE_SOURCES_NONE)
        decoder.setForceDelete(true)
        // decode resources to cache directory
        decoder.setOutDir(File(resourceCacheDirectory))
        decoder.decode()

        // get package info
        packageName = decoder.resTable.packageOriginal
        packageVersion = decoder.resTable.versionInfo.versionName

        // read dex files
        val dexFile = MultiDexIO.readDexFile(true, inputFile, NAMER, null, null)
        opcodes = dexFile.opcodes

        // save to patcher data
        patcherData = PatcherData(dexFile.classes.toMutableList(), resourceCacheDirectory)
    }

    /**
     * Add additional dex file container to the patcher.
     * @param files The dex file containers to add to the patcher.
     * @param allowedOverwrites A list of class types that are allowed to be overwritten.
     * @param throwOnDuplicates If this is set to true, the patcher will throw an exception if a duplicate class has been found.
     */
    fun addFiles(
        files: Iterable<File>,
        allowedOverwrites: Iterable<String> = emptyList(),
        throwOnDuplicates: Boolean = false
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
        if (patchResources) {
            val extDir = ExtFile(resourceCacheDirectory)
            androlib.buildResources(extDir, androlib.readMetaFile(extDir).usesFramework)
        }

        // write dex modified files
        val output = mutableMapOf<String, MemoryDataStore>()
        MultiDexIO.writeDexFile(
            true, -1, // core count
            output, NAMER, newDexFile,
            DexIO.DEFAULT_MAX_DEX_POOL_SIZE,
            null
        )
        return output
    }

    /**
     * Add a patch to the patcher.
     * @param patches The patches to add.
     */
    fun addPatches(patches: Iterable<Patch<Data>>) {
        patcherData.patches.addAll(patches)
    }

    /**
     * Resolves all signatures.
     */
    fun resolveSignatures(): List<MethodSignature> {
        val signatures = buildList {
            for (patch in patcherData.patches) {
                if (patch !is BytecodePatch) continue
                this.addAll(patch.signatures)
            }
        }
        if (signatures.isEmpty()) {
            return emptyList()
        }

        SignatureResolver(patcherData.bytecodeData.classes.internalClasses, signatures).resolve(patcherData)
        signaturesResolved = true
        return signatures
    }

    /**
     * Apply patches loaded into the patcher.
     * @param stopOnError If true, the patches will stop on the first error.
     * @return A map of [PatchResultSuccess]. If the [Patch] was successfully applied,
     * [PatchResultSuccess] will always be returned to the wrapping Result object.
     * If the [Patch] failed to apply, an Exception will always be returned to the wrapping Result object.
     */
    fun applyPatches(
        stopOnError: Boolean = false,
        callback: (String) -> Unit = {}
    ): Map<PatchMetadata, Result<PatchResultSuccess>> {
        if (!signaturesResolved) {
            resolveSignatures()
        }
        return buildMap {
            for (patch in patcherData.patches) {
                val resourcePatch = patch is ResourcePatch
                if (!patchResources && resourcePatch) continue

                callback(patch.metadata.shortName)
                val result: Result<PatchResultSuccess> = try {
                    val data = if (resourcePatch) {
                        patcherData.resourceData
                    } else {
                        patcherData.bytecodeData
                    }

                    val pr = patch.execute(data)

                    if (pr.isSuccess()) {
                        Result.success(pr.success()!!)
                    } else {
                        Result.failure(Exception(pr.error()?.errorMessage() ?: "Unknown error"))
                    }
                } catch (e: Exception) {
                    Result.failure(e)
                }
                this[patch.metadata] = result
                if (result.isFailure && stopOnError) break
            }
        }
    }
}
