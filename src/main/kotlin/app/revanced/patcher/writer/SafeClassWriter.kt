package app.revanced.patcher.writer

import org.objectweb.asm.ClassReader
import org.objectweb.asm.ClassWriter
import org.objectweb.asm.Opcodes
import java.io.IOException

/**
 * A ClassWriter that computes the common super class of two classes without
 * actually loading them with a ClassLoader.
 *
 * @author Eric Bruneton
 */
// TODO(Sculas): should we add the ClassReader parameter back?
class SafeClassWriter(flags: Int) : ClassWriter(flags) {
    override fun getCommonSuperClass(type1: String, type2: String): String {
        try {
            val info1 = typeInfo(type1)
            val info2 = typeInfo(type2)
            if (info1.access and Opcodes.ACC_INTERFACE != 0) {
                return if (typeImplements(type2, info2, type1)) {
                    type1
                } else {
                    "java/lang/Object"
                }
            }
            if (info2.access and Opcodes.ACC_INTERFACE != 0) {
                return if (typeImplements(type1, info1, type2)) {
                    type2
                } else {
                    "java/lang/Object"
                }
            }
            val b1 = typeAncestors(type1, info1)
            val b2 = typeAncestors(type2, info2)
            var result = "java/lang/Object"
            var end1 = b1.length
            var end2 = b2.length
            while (true) {
                val start1 = b1.lastIndexOf(";", end1 - 1)
                val start2 = b2.lastIndexOf(";", end2 - 1)
                if (start1 != -1 && start2 != -1 && end1 - start1 == end2 - start2) {
                    val p1 = b1.substring(start1 + 1, end1)
                    val p2 = b2.substring(start2 + 1, end2)
                    if (p1 == p2) {
                        result = p1
                        end1 = start1
                        end2 = start2
                    } else {
                        return result
                    }
                } else {
                    return result
                }
            }
        } catch (e: IOException) {
            throw RuntimeException(e.toString())
        }
    }

    /**
     * Returns the internal names of the ancestor classes of the given type.
     *
     * @param _type
     * the internal name of a class or interface.
     * @param _info
     * the ClassReader corresponding to 'type'.
     * @return a StringBuilder containing the ancestor classes of 'type',
     * separated by ';'. The returned string has the following format:
     * ";type1;type2 ... ;typeN", where type1 is 'type', and typeN is a
     * direct subclass of Object. If 'type' is Object, the returned
     * string is empty.
     * @throws IOException
     * if the bytecode of 'type' or of some of its ancestor class
     * cannot be loaded.
     */
    @Throws(IOException::class)
    private fun typeAncestors(_type: String, _info: ClassReader): StringBuilder {
        var type = _type
        var info = _info
        val b = StringBuilder()
        while ("java/lang/Object" != type) {
            b.append(';').append(type)
            type = info.superName
            info = typeInfo(type)
        }
        return b
    }

    /**
     * Returns true if the given type implements the given interface.
     *
     * @param _type
     * the internal name of a class or interface.
     * @param _info
     * the ClassReader corresponding to 'type'.
     * @param itf
     * the internal name of a interface.
     * @return true if 'type' implements directly or indirectly 'itf'
     * @throws IOException
     * if the bytecode of 'type' or of some of its ancestor class
     * cannot be loaded.
     */
    @Throws(IOException::class)
    private fun typeImplements(_type: String, _info: ClassReader, itf: String): Boolean {
        var type = _type
        var info = _info
        while ("java/lang/Object" != type) {
            info.interfaces.forEach {
                if (it == itf) {
                    return true
                }
            }
            info.interfaces.forEach {
                if (typeImplements(it, typeInfo(it), itf)) {
                    return true
                }
            }
            type = info.superName
            info = typeInfo(type)
        }
        return false
    }

    /**
     * Returns a ClassReader corresponding to the given class or interface.
     *
     * @param type
     * the internal name of a class or interface.
     * @return the ClassReader corresponding to 'type'.
     * @throws IOException
     * if the bytecode of 'type' cannot be loaded.
     */
    @Throws(IOException::class)
    private fun typeInfo(type: String): ClassReader {
        val input = ClassLoader.getSystemClassLoader().getResourceAsStream("$type.class")
            ?: throw IOException("Cannot create ClassReader for type $type")
        return input.use(::ClassReader)
    }
}