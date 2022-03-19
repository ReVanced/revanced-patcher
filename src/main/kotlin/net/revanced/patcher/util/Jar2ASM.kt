package net.revanced.patcher.util

import org.objectweb.asm.ClassReader
import org.objectweb.asm.tree.ClassNode
import java.io.InputStream
import java.util.jar.JarInputStream

object Jar2ASM {
    fun jar2asm(input: InputStream): List<ClassNode> {
        return buildList {
            val jar = JarInputStream(input)
            while (true) {
                val e = jar.nextJarEntry ?: break
                if (e.name.endsWith(".class")) {
                    val classNode = ClassNode()
                    ClassReader(jar.readAllBytes()).accept(classNode, ClassReader.EXPAND_FRAMES)
                    this.add(classNode)
                }
            }
        }
    }
}