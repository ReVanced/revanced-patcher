package net.revanced.patcher

import net.revanced.patcher.patch.Patch
import net.revanced.patcher.signature.Signature
import net.revanced.patcher.store.ASMStore
import net.revanced.patcher.store.PatchStore
import net.revanced.patcher.util.Jar2ASM
import java.io.InputStream
import java.lang.IllegalStateException

/**
 * The patcher. (docs WIP)
 *
 * @param input the input stream to read from, must be a JAR file (for now)
 */
class Patcher(
    input: InputStream,
    private val signatures: Array<Signature>,
    patches: Array<Patch>,
) {
    private val patchStore = PatchStore()
    private val asmStore = ASMStore()

    private val scanned = false

    init {
        patchStore.addPatches(*patches)
        loadJar(input)
    }

    fun scan() {
        val methods = PatternScanner(signatures).resolve()
    }

    fun patch(): String? {
        if (!scanned) throw IllegalStateException("Pattern scanner not yet ran")
        for ((_, patch) in patchStore.patches) {
            val result = patch.execute()
            if (result.isSuccess()) continue
            return result.error()!!.errorMessage()
        }
        return null
    }

    private fun loadJar(input: InputStream) {
        asmStore.classes.putAll(Jar2ASM.jar2asm(input))
    }
}