package net.revanced.patcher

import net.revanced.patcher.patch.Patch
import net.revanced.patcher.signature.Signature
import net.revanced.patcher.store.PatchStore
import net.revanced.patcher.store.SignatureStore
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
}