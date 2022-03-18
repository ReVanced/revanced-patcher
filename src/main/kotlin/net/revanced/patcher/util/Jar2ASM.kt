package net.revanced.patcher.util

import org.objectweb.asm.tree.ClassNode
import java.io.InputStream
import java.util.jar.JarInputStream

object Jar2ASM {
    fun jar2asm(input: InputStream): Map<String, ClassNode> {
        return buildMap {
            val jar = JarInputStream(input)
            var e = jar.nextJarEntry
            while (e != null) {
                TODO("Read jar file ...")
                e = jar.nextJarEntry
            }
        }
    }
}