package net.revanced.patcher

import net.revanced.patcher.signature.model.Signature
import org.objectweb.asm.tree.InsnList
import org.objectweb.asm.tree.MethodNode

internal class MethodResolver(private val targetMethods: List<MethodNode>, private val signatures: List<Signature>)  {
    fun resolve(): MutableMap<String, MethodNode> {
        val methods = mutableMapOf<String, MethodNode>()

        for (signature in signatures) {
            val method = targetMethods.firstOrNull { method ->
                method.access == signature.accessors &&
                        signature.parameters.all { parameter ->
                            method.parameters.any { methodParameter ->
                                true //TODO check for parameter element type
                            }
                        } && method.instructions.scanFor(signature.opcodes)
            } ?: continue
            methods[signature.name] = method
        }

        return methods
    }
}

//TODO: implement returning the index of the needle in the hay
private fun InsnList.scanFor(pattern: Array<Int>): Boolean {
    for (i in 0 until this.size()) {
        var occurrence = 0
        while (i + occurrence < this.size()) {
            if (this.get(i + occurrence).opcode != pattern.get(occurrence)) break
            if (++occurrence >= pattern.size) return true
        }
    }

    return false
}
