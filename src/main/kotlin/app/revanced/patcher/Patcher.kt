package app.revanced.patcher

import app.revanced.patcher.cache.Cache
import app.revanced.patcher.patch.Patch
import app.revanced.patcher.resolver.MethodResolver
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
        // TODO: find a way to load all dex classes, the code below only loads the first .dex file
        val dexFile = MultiDexIO.readDexFile(true, input, BasicDexFileNamer(), Opcodes.getDefault(), null)
        cache = Cache(dexFile.classes, MethodResolver(dexFile.classes, signatures).resolve())
    }

    fun save() {
        val newDexFile = object : DexFile {
            override fun getClasses(): MutableSet<out ClassDef> {
                // TODO: find a way to return a set with a custom iterator
                // TODO: the iterator would return the proxied class matching the current index of the list
                // TODO: instead of the original class
                for (classProxy in cache.classProxy) {
                    if (!classProxy.proxyused) continue
                    // TODO: merge this class with cache.classes somehow in an iterator
                    classProxy.mutatedClass
                }
                return cache.classes.toMutableSet()
            }

            override fun getOpcodes(): Opcodes {
                // TODO find a way to get the opcodes format
                return Opcodes.getDefault()
            }
        }

        // TODO: not sure about maxDexPoolSize & we should use the multithreading overload for writeDexFile
        MultiDexIO.writeDexFile(true, output, BasicDexFileNamer(), newDexFile, 10, null)
    }

    fun addPatches(vararg patches: Patch) {
        this.patches.addAll(patches)
    }

    fun applyPatches(stopOnError: Boolean = false): Map<String, Result<Nothing?>> {
        return buildMap {
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
