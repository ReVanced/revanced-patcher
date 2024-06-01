@file:Suppress("unused")

package app.revanced.patcher.util

import app.revanced.patcher.patch.BytecodePatchContext
import app.revanced.patcher.util.MethodNavigator.NavigateException
import app.revanced.patcher.util.proxy.mutableTypes.MutableMethod
import com.android.tools.smali.dexlib2.iface.ClassDef
import com.android.tools.smali.dexlib2.iface.Method
import com.android.tools.smali.dexlib2.iface.instruction.Instruction
import com.android.tools.smali.dexlib2.iface.instruction.ReferenceInstruction
import com.android.tools.smali.dexlib2.iface.reference.MethodReference
import com.android.tools.smali.dexlib2.util.MethodUtil

/**
 * A navigator for methods.
 *
 * @param context The [BytecodePatchContext] to use.
 * @param startMethod The [Method] to start navigating from.
 *
 * @constructor Creates a new [MethodNavigator].
 *
 * @throws NavigateException If the method does not have an implementation.
 * @throws NavigateException If the instruction at the specified index is not a method reference.
 */
class MethodNavigator internal constructor(private val context: BytecodePatchContext, private var startMethod: MethodReference) {
    private var lastNavigatedMethodReference = startMethod

    // TODO: Remove this once extension functions are supported for immutable methods.
    private val lastNavigatedMethodInstructions get() = with(immutable()) {
        implementation?.instructions ?: throw NavigateException("Method $definingClass.$name does not have an implementation.")
    }

    /**
     * Navigate to the method at the specified index.
     *
     * @param index The index of the method to navigate to.
     *
     * @return This [MethodNavigator].
     */
    fun at(vararg index: Int): MethodNavigator {
        index.forEach {
            lastNavigatedMethodReference = lastNavigatedMethodInstructions.getMethodReferenceAt(it)
        }

        return this
    }

    /**
     * Navigate to the method at the specified index that matches the specified predicate.
     *
     * @param index The index of the method to navigate to.
     * @param predicate The predicate to match.
     */
    fun at(index: Int = 0, predicate: (Instruction) -> Boolean): MethodNavigator {
        lastNavigatedMethodReference = lastNavigatedMethodInstructions.asSequence()
            .filter(predicate).asIterable().getMethodReferenceAt(index)

        return this
    }

    /**
     * Get the method reference at the specified index.
     *
     * @param index The index of the method reference to get.
     */
    private fun Iterable<Instruction>.getMethodReferenceAt(index: Int): MethodReference {
        val instruction = elementAt(index) as? ReferenceInstruction
            ?: throw NavigateException("Instruction at index $index is not a method reference.")

        return instruction.reference as MethodReference
    }

    /**
     * Get the last navigated method mutably.
     *
     * @return The last navigated method mutably.
     */
    fun mutable() = context.classBy(matchesCurrentMethodReferenceDefiningClass)!!.mutableClass.firstMethodBySignature
        as MutableMethod

    /**
     * Get the last navigated method immutably.
     *
     * @return The last navigated method immutably.
     */
    fun immutable() = context.classes.first(matchesCurrentMethodReferenceDefiningClass).firstMethodBySignature

    /**
     * Predicate to match the class defining the current method reference.
     */
    private val matchesCurrentMethodReferenceDefiningClass = { classDef: ClassDef ->
        classDef.type == lastNavigatedMethodReference.definingClass
    }

    /**
     * Find the first [lastNavigatedMethodReference] in the class.
     */
    private val ClassDef.firstMethodBySignature get() = methods.first {
        MethodUtil.methodSignaturesMatch(it, lastNavigatedMethodReference)
    }

    /**
     * An exception thrown when navigating fails.
     *
     * @param message The message of the exception.
     */
    internal class NavigateException internal constructor(message: String) : Exception(message)
}
