package net.revanced.patcher.resolver

import mu.KotlinLogging
import net.revanced.patcher.cache.PatchData
import net.revanced.patcher.cache.ScanData
import net.revanced.patcher.signature.Signature
import org.objectweb.asm.Type
import org.objectweb.asm.tree.ClassNode
import org.objectweb.asm.tree.InsnList
import org.objectweb.asm.tree.MethodNode

private val logger = KotlinLogging.logger("MethodResolver")

internal class MethodResolver(private val classList: List<ClassNode>, private val signatures: Array<Signature>)  {
    fun resolve(): MutableMap<String, PatchData> {
        val patchData = mutableMapOf<String, PatchData>()

        for ((classNode, methods) in classList) {
            for (method in methods) {
                for (signature in signatures) {
                    if (patchData.containsKey(signature.name)) { // method already found for this sig
                        logger.debug { "Sig ${signature.name} already found, skipping." }
                        continue
                    }
                    logger.debug { "Resolving sig ${signature.name}: ${classNode.name} / ${method.name}" }
                    val (r, sr) = this.cmp(method, signature)
                    if (!r || sr == null) {
                        logger.debug { "Compare result for sig ${signature.name} has failed!" }
                        continue
                    }
                    logger.debug { "Method for sig ${signature.name} found!" }
                    patchData[signature.name] = PatchData(
                        classNode,
                        method,
                        ScanData(
                            // sadly we cannot create contracts for a data class, so we must assert
                            sr.startIndex!!,
                            sr.endIndex!!
                        )
                    )
                }
            }
        }

        for (signature in signatures) {
            if (patchData.containsKey(signature.name)) continue
            logger.error { "Could not find method for sig ${signature.name}!" }
        }

        return patchData
    }

    private fun cmp(method: MethodNode, signature: Signature): Pair<Boolean, ScanResult?> {
        if (signature.returns != Type.getReturnType(method.desc)) {
            logger.debug { "Comparing sig ${signature.name}: invalid return type:\nexpected ${signature.returns},\ngot ${Type.getReturnType(method.desc)}" }
            return false to null
        }
        if (signature.accessors != method.access) {
            logger.debug { "Comparing sig ${signature.name}: invalid accessors:\nexpected ${signature.accessors},\ngot ${method.access}" }
            return false to null
        }
        if (!signature.parameters.contentEquals(Type.getArgumentTypes(method.desc))) {
            logger.debug { "Comparing sig ${signature.name}: invalid parameter types:\nexpected ${signature.parameters},\ngot ${Type.getArgumentTypes(method.desc)}" }
            return false to null
        }

        val result = method.instructions.scanFor(signature.opcodes)
        if (!result.found) {
            logger.debug { "Comparing sig ${signature.name}: invalid opcode pattern" }
            return false to null
        }

        return true to result
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
