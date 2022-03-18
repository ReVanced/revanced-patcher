package net.revanced.patcher

import net.revanced.patcher.patch.Patch
import net.revanced.patcher.signature.Signature
import net.revanced.patcher.store.PatchStore
import net.revanced.patcher.store.SignatureStore
import org.objectweb.asm.Opcodes
import java.io.InputStream

class Patcher(
    private val input: InputStream,
    signatures: Array<Signature>,
    patches: Array<Patch>,
) {
    init {
        SignatureStore.addSignatures(*signatures)
        PatchStore.addPatches(*patches)
    }

    fun patch(): String? {
        for (patch in PatchStore.patches) {
            val result = patch.execute()
            if (result.isSuccess()) continue
            return result.error()!!.errorMessage()
        }
        return null
    }
}