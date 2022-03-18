package net.revanced.patcher

import net.revanced.patcher.patch.Patch
import net.revanced.patcher.signature.Signature
import net.revanced.patcher.store.MethodStore
import net.revanced.patcher.store.PatchStore
import java.io.InputStream
import java.lang.IllegalStateException

class Patcher(
    private val input: InputStream,
    private val signatures: Array<Signature>,
    patches: Array<Patch>,
) {
    private val patchStore = PatchStore()
    private val methodStore = MethodStore()

    private val scanned = false

    init {
        patchStore.addPatches(*patches)
    }

    fun scan() {
        // methodStore.methods = PatternScanner(signatures).resolve()
    }

    fun patch(): String? {
        if (!scanned) throw IllegalStateException("Pattern scanner not yet ran")
        for (patch in patchStore.patches) {
            val result = patch.execute()
            if (result.isSuccess()) continue
            return result.error()!!.errorMessage()
        }
        return null
    }
}