package app.revanced.patcher.util.method

import app.revanced.patcher.BytecodeContext
import app.revanced.patcher.util.proxy.mutableTypes.MutableMethod
import org.jf.dexlib2.iface.Method
import org.jf.dexlib2.iface.instruction.ReferenceInstruction
import org.jf.dexlib2.iface.reference.MethodReference
import org.jf.dexlib2.util.MethodUtil

/**
 * Find a method from another method via instruction offsets.
 * @param bytecodeContext The context to use when resolving the next method reference.
 * @param currentMethod The method to start from.
 */
class MethodWalker internal constructor(
    private val bytecodeContext: BytecodeContext,
    private var currentMethod: Method
) {
    /**
     * Get the method which was walked last.
     *
     * It is possible to cast this method to a [MutableMethod], if the method has been walked mutably.
     *
     * @return The method which was walked last.
     */
    fun getMethod(): Method {
        return currentMethod
    }

    /**
     * Walk to a method defined at the offset in the instruction list of the current method.
     *
     * The current method will be mutable.
     *
     * @param offset The offset of the instruction. This instruction must be of format 35c.
     * @param walkMutable If this is true, the class of the method will be resolved mutably.
     * @return The same [MethodWalker] instance with the method at [offset].
     */
    fun nextMethod(offset: Int, walkMutable: Boolean = false): MethodWalker {
        currentMethod.implementation?.instructions?.let { instructions ->
            val instruction = instructions.elementAt(offset)

            val newMethod = (instruction as ReferenceInstruction).reference as MethodReference
            val proxy = bytecodeContext.classes.findClassProxied(newMethod.definingClass)!!

            val methods = if (walkMutable) proxy.mutableClass.methods else proxy.immutableClass.methods
            currentMethod = methods.first {
                return@first MethodUtil.methodSignaturesMatch(it, newMethod)
            }
            return this
        }
        throw MethodNotFoundException("This method can not be walked at offset $offset inside the method ${currentMethod.name}")
    }

    internal class MethodNotFoundException(exception: String) : Exception(exception)
}