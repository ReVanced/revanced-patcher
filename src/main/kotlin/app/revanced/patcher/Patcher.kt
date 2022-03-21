package app.revanced.patcher

import app.revanced.patcher.cache.Cache
import app.revanced.patcher.patch.Patch
import app.revanced.patcher.resolver.MethodResolver
import app.revanced.patcher.signature.Signature
import app.revanced.patcher.util.Io
import org.objectweb.asm.tree.ClassNode
import java.io.IOException
import java.io.InputStream
import java.io.OutputStream

/**
 * The Patcher class.
 * ***It is of utmost importance that the input and output streams are NEVER closed.***
 *
 * @param input the input stream to read from, must be a JAR
 * @param output the output stream to write to
 * @param signatures the signatures
 * @sample app.revanced.patcher.PatcherTest
 * @throws IOException if one of the streams are closed
 */
class Patcher(
    private val input: InputStream,
    private val output: OutputStream,
    signatures: Array<Signature>,
) {
    var cache: Cache

    private var io: Io
    private val patches = mutableListOf<Patch>()

    init {
        val classes = mutableListOf<ClassNode>()
        io = Io(input, output, classes)
        io.readFromJar()
        cache = Cache(classes, MethodResolver(classes, signatures).resolve())
    }

    /**
     * Saves the output to the output stream.
     * Calling this method will close the input and output streams,
     * meaning this method should NEVER be called after.
     *
     * @throws IOException if one of the streams are closed
     */
    fun save() {
        io.saveAsJar()
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