package app.revanced.patcher

import app.revanced.patcher.patch.Patch
import app.revanced.patcher.patch.PatchMetadata
import app.revanced.patcher.patch.PatchResultSuccess
import app.revanced.patcher.signature.MethodSignature
import app.revanced.patcher.signature.resolver.SignatureResolver
import app.revanced.patcher.util.ListBackedSet
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
 * ReVanced Patcher.
 * @param input The input file (an apk or any other multi dex container).
 */
class Patcher(
    input: File,
) {
    private val patcherData: PatcherData
    private val opcodes: Opcodes
    private var signaturesResolved = false

    init {
        val dexFile = MultiDexIO.readDexFile(true, input, NAMER, null, null)
        opcodes = dexFile.opcodes
        patcherData = PatcherData(dexFile.classes.toMutableList())
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
                val e = patcherData.classes.internalClasses.findIndexed { it.type == classDef.type }
                if (e != null) {
                    if (throwOnDuplicates) {
                        throw Exception("Class ${classDef.type} has already been added to the patcher.")
                    }
                    val (_, idx) = e
                    if (allowedOverwrites.contains(classDef.type)) {
                        patcherData.classes.internalClasses[idx] = classDef
                    }
                    continue
                }
                patcherData.classes.internalClasses.add(classDef)
            }
        }
    }

    /**
     * Save the patched dex file.
     */
    fun save(): Map<String, MemoryDataStore> {
        val newDexFile = object : DexFile {
            override fun getClasses(): Set<ClassDef> {
                patcherData.classes.applyProxies()
                return ListBackedSet(patcherData.classes.internalClasses)
            }

            override fun getOpcodes(): Opcodes {
                return this@Patcher.opcodes
            }
        }

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
    fun addPatches(patches: Iterable<Patch>) {
        patcherData.patches.addAll(patches)
    }

    /**
     * Resolves all signatures.
     * @throws IllegalStateException if signatures have already been resolved.
     */
    fun resolveSignatures(): List<MethodSignature> {
        if (signaturesResolved) {
            throw IllegalStateException("Signatures have already been resolved.")
        }

        val signatures = patcherData.patches.flatMap { it.signatures }

        if (signatures.isEmpty()) return emptyList()

        SignatureResolver(patcherData.classes.internalClasses, signatures).resolve(patcherData)
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
        if (!signaturesResolved && patcherData.patches.isNotEmpty()) {
            resolveSignatures()
        }
        return buildMap {
            for (patch in patcherData.patches) {
                callback(patch.metadata.shortName)
                val result: Result<PatchResultSuccess> = try {
                    val pr = patch.execute(patcherData)
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
