package net.revanced.patcher

import net.revanced.patcher.cache.Cache
import net.revanced.patcher.patch.Patch
import net.revanced.patcher.resolver.MethodResolver
import net.revanced.patcher.signature.Signature
import net.revanced.patcher.util.Io
import java.io.ByteArrayOutputStream
import java.io.InputStream
import java.io.OutputStream

/**
 * The patcher. (docs WIP)
 *
 * @param input the input stream to read from, must be a JAR
 * @param signatures the signatures
 * @sample net.revanced.patcher.PatcherTest
 */
class Patcher(
    private val input: InputStream,
    signatures: Array<Signature>,
) {
    var cache: Cache
    private val patches: MutableList<Patch> = mutableListOf()
    private var inputBytes: ByteArray

    init {
        // Copy input stream or else there is no way to reset it (required because we read multiple times)
        val inputCopy = ByteArrayOutputStream()
        val buffer = ByteArray(1024)
        var len: Int
        while (input.read(buffer).also { len = it } > -1) {
            inputCopy.write(buffer, 0, len)
        }
        inputCopy.flush()
        inputBytes = inputCopy.toByteArray()
        inputCopy.close()

        val classes = Io.readClassesFromJar(inputBytes)
        cache = Cache(classes, MethodResolver(classes, signatures).resolve())
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

    fun saveTo(output: OutputStream) {
        Io.writeClassesToJar(inputBytes, output, cache.classes)
    }
}