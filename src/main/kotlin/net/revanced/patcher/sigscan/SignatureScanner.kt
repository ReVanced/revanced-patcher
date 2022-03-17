package net.revanced.patcher.sigscan

import net.revanced.patcher.signatures.Signature
import org.jf.dexlib2.dexbacked.DexBackedMethod
import org.jf.dexlib2.iface.Method


class SignatureScanner(methods: List<Method>) {
    fun resolve(signature: Array<out Signature>) : List<DexBackedMethod> {
        TODO()
    }
}