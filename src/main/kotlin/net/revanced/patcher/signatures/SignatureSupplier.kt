package net.revanced.patcher.signatures

interface SignatureSupplier {
    fun signatures(): Array<Signature>
}