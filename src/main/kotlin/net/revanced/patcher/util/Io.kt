package net.revanced.patcher.util

import org.objectweb.asm.ClassReader
import org.objectweb.asm.ClassWriter
import org.objectweb.asm.tree.ClassNode
import java.io.InputStream
import java.io.OutputStream
import java.util.jar.JarEntry
import java.util.jar.JarInputStream
import java.util.jar.JarOutputStream

object Io {
    fun readClassesFromJar(input: InputStream) = mutableListOf<ClassNode>().apply {
        val jar = JarInputStream(input)
            while (true) {
                val e = jar.nextJarEntry ?: break
                if (e.name.endsWith(".class")) {
                    val classNode = ClassNode()
                    ClassReader(jar.readAllBytes()).accept(classNode, ClassReader.EXPAND_FRAMES)
                        this.add(classNode)
                }
                jar.closeEntry()
            }
    }

    fun writeClassesToJar(input: InputStream, output: OutputStream, classes: List<ClassNode>) {
        val jis = JarInputStream(input)
        val jos = JarOutputStream(output)

        // TODO: Add support for adding new/custom classes
        while (true) {
            val next = jis.nextJarEntry ?: break
            val e = JarEntry(next) // clone it, to not modify the input (if possible)
            jos.putNextEntry(e)

            val clazz = classes.singleOrNull {
                    clazz -> clazz.name == e.name
            };
            if (clazz != null) {
                val cw = ClassWriter(ClassWriter.COMPUTE_MAXS or ClassWriter.COMPUTE_FRAMES)
                clazz.accept(cw)
                jos.write(cw.toByteArray())
            } else {
                jos.write(jis.readAllBytes())
            }
            jos.closeEntry()
        }
    }
}