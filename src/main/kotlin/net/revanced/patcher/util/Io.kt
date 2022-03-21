package net.revanced.patcher.util

import org.objectweb.asm.ClassReader
import org.objectweb.asm.ClassWriter
import org.objectweb.asm.tree.ClassNode
import java.io.BufferedInputStream
import java.io.InputStream
import java.io.OutputStream
import java.util.jar.JarEntry
import java.util.jar.JarInputStream
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream

internal class Io(
    private val input: InputStream,
    private val output: OutputStream,
    private val classes: MutableList<ClassNode>
) {
    private val bufferedInputStream = BufferedInputStream(input)

    fun readFromJar() {
        bufferedInputStream.mark(0)
        // create a BufferedInputStream in order to read the input stream again when calling saveAsJar(..)
        val jis = JarInputStream(bufferedInputStream)

        // read all entries from the input stream
        // we use JarEntry because we only read .class files
        lateinit var jarEntry: JarEntry
        while (jis.nextJarEntry.also { if (it != null) jarEntry = it } != null) {
            // if the current entry ends with .class (indicating a java class file), add it to our list of classes to return
            if (jarEntry.name.endsWith(".class")) {
                // create a new ClassNode
                val classNode = ClassNode()
                // read the bytes with a ClassReader into the ClassNode
                ClassReader(jis.readBytes()).accept(classNode, ClassReader.EXPAND_FRAMES)
                // add it to our list
                classes.add(classNode)
            }

            // finally, close the entry
            jis.closeEntry()
        }

        // at last reset the buffered input stream
        bufferedInputStream.reset()
    }

    fun saveAsJar() {
        val jis = ZipInputStream(bufferedInputStream)
        val jos = ZipOutputStream(output)

        // first write all non .class zip entries from the original input stream to the output stream
        // we read it first to close the input stream as fast as possible
        // TODO(oSumAtrIX): There is currently no way to remove non .class files.
        lateinit var zipEntry: ZipEntry
        while (jis.nextEntry.also { if (it != null) zipEntry = it } != null) {
            // skip all class files because we added them in the loop above
            // TODO(oSumAtrIX): Check for zipEntry.isDirectory
            if (zipEntry.name.endsWith(".class")) continue

            // create a new zipEntry and write the contents of the zipEntry to the output stream
            jos.putNextEntry(ZipEntry(zipEntry))
            jos.write(jis.readBytes())

            // close the newly created zipEntry
            jos.closeEntry()
        }

        // finally, close the input stream
        jis.close()
        bufferedInputStream.close()
        input.close()

        // now write all the patched classes to the output stream
        for (patchedClass in classes) {
            // create a new entry of the patched class
            jos.putNextEntry(JarEntry(patchedClass.name + ".class"))

            // parse the patched class to a byte array and write it to the output stream
            val cw = ClassWriter(ClassWriter.COMPUTE_MAXS or ClassWriter.COMPUTE_FRAMES)
            patchedClass.accept(cw)
            jos.write(cw.toByteArray())

            // close the newly created jar entry
            jos.closeEntry()
        }

        // finally, close the rest of the streams
        jos.close()
        output.close()
    }
}