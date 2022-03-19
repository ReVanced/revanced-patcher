package net.revanced.patcher.util

import org.objectweb.asm.ClassReader
import org.objectweb.asm.ClassWriter
import org.objectweb.asm.tree.ClassNode
import java.io.InputStream
import java.io.OutputStream
import java.util.jar.JarEntry
import java.util.jar.JarInputStream
import java.util.jar.JarOutputStream

object Jar2ASM {
    fun jar2asm(input: InputStream): Map<String, ClassNode> {
        return buildMap {
            val jar = JarInputStream(input)
            while (true) {
                val e = jar.nextJarEntry ?: break
                if (e.name.endsWith(".class")) {
                    val classNode = ClassNode()
                    ClassReader(jar.readAllBytes()).accept(classNode, ClassReader.EXPAND_FRAMES)
                    this[e.name] = classNode
                }
                jar.closeEntry()
            }
        }
    }
    fun asm2jar(input: InputStream, output: OutputStream, structure: Map<String, ClassNode>) {
        val jis = JarInputStream(input)
        val jos = JarOutputStream(output)

        // TODO: Add support for adding new/custom classes
        while (true) {
            val next = jis.nextJarEntry ?: break
            val e = JarEntry(next) // clone it, to not modify the input (if possible)
            jos.putNextEntry(e)
            if (structure.containsKey(e.name)) {
                val cw = ClassWriter(ClassWriter.COMPUTE_MAXS or ClassWriter.COMPUTE_FRAMES)
                val cn = structure[e.name]!!
                cn.accept(cw)
                jos.write(cw.toByteArray())
            } else {
                jos.write(jis.readAllBytes())
            }
            jos.closeEntry()
        }
    }
}