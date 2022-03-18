package net.revanced.patcher.store

import net.revanced.patcher.signature.Signature

object SignatureStore {
    private val signatures: MutableList<Signature> = mutableListOf()

    fun addSignatures(vararg signatures: Signature) {
        this.signatures.addAll(signatures)
    }
}