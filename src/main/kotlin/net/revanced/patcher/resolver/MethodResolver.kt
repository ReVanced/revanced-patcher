package net.revanced.patcher.resolver

import net.revanced.patcher.cache.PatchData
import net.revanced.patcher.signature.Signature
import org.objectweb.asm.Type
import org.objectweb.asm.tree.ClassNode
import org.objectweb.asm.tree.InsnList
import org.objectweb.asm.tree.MethodNode

internal class MethodResolver(private val classList: List<ClassNode>, private val signatures: Array<Signature>)  {
    fun resolve(): MutableMap<String, PatchData> {
        val patchData = mutableMapOf<String, PatchData>()

        for ((classNode, methods) in classList) {
            for (method in methods) {
                for (signature in signatures) {
                    if (patchData.containsKey(signature.name)) continue // method already found for this sig
                    if (!this.cmp(method, signature)) continue
                    patchData[signature.name] = PatchData(classNode, method)
                }
            }
        }

        for (signature in signatures) {
            if (patchData.containsKey(signature.name)) continue
            // not found
            // TODO log error or whatever
        }

        return patchData
    }

    private fun cmp(method: MethodNode, signature: Signature): Boolean {
        if (signature.returns != Type.getReturnType(method.desc)) return false
        if (signature.accessors != method.access) return false
        if (!signature.parameters.contentEquals(Type.getArgumentTypes(method.desc))) return false

        val result = method.instructions.scanFor(signature.opcodes)
        if (!result.found) return false
        // TODO make use of the startIndex and endIndex we have from the result

        return true
    }
}

private operator fun ClassNode.component1(): ClassNode {
    return this
}

private operator fun ClassNode.component2(): List<MethodNode> {
    return this.methods
}

private fun InsnList.scanFor(pattern: Array<Int>): ScanResult {
    for (i in 0 until this.size()) {
        var occurrence = 0
        while (i + occurrence < this.size()) {
            val current = i + occurrence
            if (this[current].opcode != pattern[occurrence]) break
            if (++occurrence >= pattern.size) {
                return ScanResult(true, current - pattern.size, current)
            }
        }
    }

    return ScanResult(false)
}
