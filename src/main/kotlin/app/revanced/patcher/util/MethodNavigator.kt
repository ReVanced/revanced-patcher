@file:Suppress("unused")

package app.revanced.patcher.util

import app.revanced.patcher.patch.BytecodePatchContext
import app.revanced.patcher.util.proxy.mutableTypes.MutableMethod
import com.android.tools.smali.dexlib2.iface.ClassDef
import com.android.tools.smali.dexlib2.iface.Method
import com.android.tools.smali.dexlib2.iface.instruction.ReferenceInstruction
import com.android.tools.smali.dexlib2.iface.reference.MethodReference
import com.android.tools.smali.dexlib2.util.MethodUtil

/**
 * A navigator for methods.
 *
 * @param context The [BytecodePatchContext] to use.
 * @param method The [Method] to navigate.
 *
 * @constructor Creates a new [MethodNavigator].
 *
 * @throws NavigateException If the method does not have an implementation.
 * @throws NavigateException If the instruction at the specified index is not a method reference.
 */
class MethodNavigator internal constructor(private val context: BytecodePatchContext, private var method: Method) {
    var reference: MethodReference = method

    /**
     * Navigate to the method at the specified index.
     *
     * @param index The index of the method to navigate to.
     *
     * @return This [MethodNavigator].
     */
    fun at(vararg index: Int): MethodNavigator {
        index.forEach {
            val currentMethod = immutable()
            val instructions = currentMethod.implementation?.instructions
                ?: throw NavigateException(
                    "Method ${currentMethod.definingClass}.${currentMethod.name} does not have an implementation.",
                )

            val instruction = instructions.elementAt(it) as? ReferenceInstruction
            val newMethod = instruction?.reference as? MethodReference
                ?: throw NavigateException("Instruction at index $it is not a method reference.")

            reference = newMethod
        }

        return this
    }

    /**
     * Get the last navigated method mutably.
     *
     * @return The last navigated method mutably.
     */
    fun mutable() = context.classBy { classDef ->
        classDef.type == reference.definingClass
    }!!.mutableClass.methodBySignature() as MutableMethod

    /**
     * Get the last navigated method immutably.
     *
     * @return The last navigated method immutably.
     */
    fun immutable() = context.classes.first { classDef ->
        classDef.type == reference.definingClass
    }.methodBySignature()

    private fun ClassDef.methodBySignature() = methods.first { MethodUtil.methodSignaturesMatch(it, method) }

    /**
     * An exception thrown when navigating fails.
     *
     * @param message The message of the exception.
     */
    internal class NavigateException internal constructor(message: String) : Exception(message)
}
