package net.revanced.patcher.util

import org.objectweb.asm.ClassReader
import org.objectweb.asm.ClassWriter
import org.objectweb.asm.tree.ClassNode
import java.io.ByteArrayInputStream
import java.io.InputStream
import java.io.OutputStream
import java.util.jar.JarEntry
import java.util.jar.JarInputStream
import java.util.jar.JarOutputStream

object Io {
    fun readClassesFromJar(input: ByteArray) = mutableListOf<ClassNode>().apply {
        val inputStream = ByteArrayInputStream(input)
        val jar = JarInputStream(inputStream)
            while (true) {
                val e = jar.nextJarEntry ?: break
                if (e.name.endsWith(".class")) {
                    val classNode = ClassNode()
                    ClassReader(jar.readBytes()).accept(classNode, ClassReader.EXPAND_FRAMES)
                    this.add(classNode)
                }
                jar.closeEntry()
            }
        jar.close()
        inputStream.close()
    }

    fun writeClassesToJar(input: ByteArray, output: OutputStream, classes: List<ClassNode>) {
        val inputStream = ByteArrayInputStream(input)
        val jis = JarInputStream(inputStream)
        val jos = JarOutputStream(output)

        // TODO: Add support for adding new/custom classes
        while (true) {
            val next = jis.nextJarEntry ?: break
            val e = JarEntry(next) // clone it, to not modify the input (if possible)
            jos.putNextEntry(e)

            val clazz = classes.singleOrNull {
                clazz -> clazz.name+".class" == e.name // clazz.name is the class name only while e.name is the full filename with extension
            };

            // TODO: write modded classes does not work currently, so always copy from input for now:
            jos.write(jis.readBytes())
//            if (clazz != null) {
//                val cw = ClassWriter(ClassWriter.COMPUTE_MAXS or ClassWriter.COMPUTE_FRAMES)
//                clazz.accept(cw)
//                jos.write(cw.toByteArray())
//            } else {
//                jos.write(jis.readBytes())
//            }

            jos.closeEntry()
        }

        jis.close()
        jos.close()
        inputStream.close()
        output.close()
    }
}