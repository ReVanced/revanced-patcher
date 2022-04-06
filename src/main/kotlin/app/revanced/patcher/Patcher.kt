package app.revanced.patcher

import app.revanced.patcher.cache.Cache
import app.revanced.patcher.patch.Patch
import app.revanced.patcher.resolver.SignatureResolver
import app.revanced.patcher.signature.MethodSignature
import lanchon.multidexlib2.BasicDexFileNamer
import lanchon.multidexlib2.DexIO
import lanchon.multidexlib2.MultiDexIO
import org.jf.dexlib2.Opcodes
import org.jf.dexlib2.iface.ClassDef
import org.jf.dexlib2.iface.DexFile
import java.io.File

/**
 * ReVanced Patcher.
 * @param input The input file (an apk or any other multi dex container).
 * @param output The output folder.
 * @param signatures An array of method signatures for the patches
 *
 */
class Patcher(
    input: File,
    private val output: File,
    signatures: Array<MethodSignature>,
) {
    private val cache: Cache
    private val patches = mutableSetOf<Patch>()

    init {
        val dexFile = MultiDexIO.readDexFile(true, input, BasicDexFileNamer(), null, null)
        cache = Cache(dexFile.classes.toMutableSet(), SignatureResolver(dexFile.classes, signatures).resolve())
    }

    /**
     * Save the patched dex file.
     */
    fun save() {
        val newDexFile = object : DexFile {
            override fun getClasses(): Set<ClassDef> {
                // this is a slow workaround for now
                cache.methodMap.values.forEach {
                    if (!it.definingClassProxy.proxyUsed) return@forEach
                    cache.classes.replace(it.definingClassProxy.originalIndex, it.definingClassProxy.mutatedClass)
                }
                cache.classProxy
                    .filter { it.proxyUsed }.forEach { proxy ->
                        cache.classes.replace(proxy.originalIndex, proxy.mutatedClass)
                    }

                return cache.classes
            }

            override fun getOpcodes(): Opcodes {
                // TODO find a way to get the opcodes format
                return Opcodes.getDefault()
            }
        }

        // TODO: we should use the multithreading capable overload for writeDexFile
        MultiDexIO.writeDexFile(true, output, BasicDexFileNamer(), newDexFile, DexIO.DEFAULT_MAX_DEX_POOL_SIZE, null)
    }

    /**
     * Add a patch to the patcher.
     * @param patches The patches to add.
     */
    fun addPatches(vararg patches: Patch) {
        this.patches.addAll(patches)
    }

    /**
     * Apply patches loaded into the patcher.
     * @param stopOnError If true, the patches will stop on the first error.
     */
    fun applyPatches(stopOnError: Boolean = false): Map<String, Result<Nothing?>> {
        return buildMap {
            // TODO: after each patch execution we could clear left overs like proxied classes to safe memory
            for (patch in patches) {
                val result: Result<Nothing?> = try {
                    val pr = patch.execute(cache)
                    if (pr.isSuccess()) continue
                    Result.failure(Exception(pr.error()?.errorMessage() ?: "Unknown error"))
                } catch (e: Exception) {
                    Result.failure(e)
                }
                this[patch.patchName] = result
                if (stopOnError && result.isFailure) break
            }
        }
    }
}

private fun MutableSet<ClassDef>.replace(originalIndex: Int, mutatedClass: ClassDef) {
    this.remove(this.elementAt(originalIndex))
    this.add(mutatedClass)
}
