package app.revanced.patcher.methodWalker

import app.revanced.patcher.MethodNotFoundException
import app.revanced.patcher.PatcherData
import app.revanced.patcher.extensions.softCompareTo
import app.revanced.patcher.proxy.mutableTypes.MutableMethod
import org.jf.dexlib2.Format
import org.jf.dexlib2.iface.Method
import org.jf.dexlib2.iface.instruction.formats.Instruction35c
import org.jf.dexlib2.iface.reference.MethodReference
import org.jf.dexlib2.util.Preconditions

/**
 * Find a method from another method via instruction offsets.
 * @param patcherData The patcherData to use when resolving the next method reference.
 * @param currentMethod The method to start from.
 */
class MethodWalker internal constructor(
    private val patcherData: PatcherData,
    private var currentMethod: Method
) {
    /**
     * Get the method which was walked last.
     * It is possible to cast this method to a [MutableMethod], if the method has been walked mutably.
     */
    fun getMethod(): Method {
        return currentMethod
    }

    /**
     * Walk to a method defined at the offset in the instruction list of the current method.
     * @param offset The offset of the instruction. This instruction must be of format 35c.
     * @param walkMutable If this is true, the class of the method will be resolved mutably.
     * The current method will be mutable.
     */
    fun walk(offset: Int, walkMutable: Boolean = false): MethodWalker {
        currentMethod.implementation?.instructions?.let { instructions ->
            val instruction = instructions.elementAt(offset)

            Preconditions.checkFormat(instruction.opcode, Format.Format35c)

            val newMethod = (instruction as Instruction35c).reference as MethodReference
            val proxy = patcherData.findClass(newMethod.definingClass)!!

            val methods = if (walkMutable) proxy.resolve().methods else proxy.immutableClass.methods
            currentMethod = methods.first { it ->
                return@first it.softCompareTo(newMethod)
            }
            return this
        }
        throw MethodNotFoundException("This method can not be walked at offset $offset inside the method ${currentMethod.name}")
    }


}