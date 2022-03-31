package app.revanced.patcher

import app.revanced.patcher.cache.Cache
import app.revanced.patcher.patch.Patch
import app.revanced.patcher.resolver.SignatureResolver
import app.revanced.patcher.signature.MethodSignature
import lanchon.multidexlib2.BasicDexFileNamer
import lanchon.multidexlib2.MultiDexIO
import org.jf.dexlib2.Opcodes
import org.jf.dexlib2.iface.ClassDef
import org.jf.dexlib2.iface.DexFile
import java.io.File

class Patcher(
    input: File,
    private val output: File,
    signatures: Array<MethodSignature>,

    ) {
    private val cache: Cache
    private val patches = mutableSetOf<Patch>()

    init {
        val dexFile = MultiDexIO.readDexFile(true, input, BasicDexFileNamer(), Opcodes.getDefault(), null)
        cache = Cache(dexFile.classes, SignatureResolver(dexFile.classes, signatures).resolve())
    }

    fun save() {
        val newDexFile = object : DexFile {
            override fun getClasses(): Set<ClassDef> {
                // this is a slow workaround for now
                val mutableClassList = cache.classes.toMutableList()
                cache.classProxy
                    .filter { it.proxyUsed }.forEach { proxy ->
                        mutableClassList[proxy.originalIndex] = proxy.mutatedClass
                    }
                return mutableClassList.toSet()
            }

            override fun getOpcodes(): Opcodes {
                // TODO find a way to get the opcodes format
                return Opcodes.getDefault()
            }
        }

        // TODO: we should use the multithreading capable overload for writeDexFile
        MultiDexIO.writeDexFile(true, output, BasicDexFileNamer(), newDexFile, 50000, null)
    }

    fun addPatches(vararg patches: Patch) {
        this.patches.addAll(patches)
    }

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
