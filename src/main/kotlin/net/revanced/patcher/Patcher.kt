package net.revanced.patcher

import net.revanced.patcher.cache.Cache
import net.revanced.patcher.patch.Patch
import net.revanced.patcher.signature.model.Signature
import org.objectweb.asm.ClassReader
import org.objectweb.asm.tree.ClassNode
import org.objectweb.asm.tree.MethodNode
import java.io.File
import java.util.jar.JarFile

class Patcher private constructor(
    file: File,
    signatures: List<Signature>
) {
    val cache = Cache()
    private val patches: MutableList<Patch> = mutableListOf()

    init {
        // collecting all methods here
        val targetMethods: MutableList<MethodNode> = mutableListOf()

        val jarFile = JarFile(file)
            jarFile.stream().forEach { jarEntry ->
            jarFile.getInputStream(jarEntry).use { jis ->
                if (jarEntry.name.endsWith(".class")) {
                    val classNode = ClassNode()
                    ClassReader(jis.readAllBytes()).accept(classNode, ClassReader.EXPAND_FRAMES)
                    targetMethods.addAll(classNode.methods)
                }
            }
        }

        // reducing to required methods via signatures
        cache.Methods = MethodResolver(targetMethods, signatures).resolve()

    }

    companion object {
        fun loadFromFile(file: String, signatures: List<Signature>): Patcher = Patcher(File(file), signatures)
    }

    fun addPatches(vararg patches: Patch) {
        this.patches.addAll(patches)
    }

    fun executePatches(): String? {
        for (patch in patches) {
            val result = patch.execute()
            if (result.isSuccess()) continue
            return result.error()!!.errorMessage()
        }
        return null
    }
}