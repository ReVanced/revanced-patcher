package app.revanced.patcher.extensions

import app.revanced.patcher.util.proxy.mutableTypes.MutableMethod
import app.revanced.patcher.util.proxy.mutableTypes.MutableMethod.Companion.toMutable
import app.revanced.patcher.util.smali.toInstruction
import app.revanced.patcher.util.smali.toInstructions
import org.jf.dexlib2.AccessFlags
import org.jf.dexlib2.builder.BuilderInstruction
import org.jf.dexlib2.builder.MutableMethodImplementation
import org.jf.dexlib2.iface.Method
import org.jf.dexlib2.iface.reference.MethodReference
import org.jf.dexlib2.immutable.ImmutableMethod
import org.jf.dexlib2.immutable.ImmutableMethodImplementation
import org.jf.dexlib2.util.MethodUtil
import java.io.OutputStream

infix fun AccessFlags.or(other: AccessFlags) = this.value or other.value
infix fun Int.or(other: AccessFlags) = this or other.value

fun MutableMethodImplementation.addInstructions(index: Int, instructions: List<BuilderInstruction>) {
    for (i in instructions.lastIndex downTo 0) {
        this.addInstruction(index, instructions[i])
    }
}

/**
 * Compare a method to another, considering constructors and parameters.
 * @param otherMethod The method to compare against.
 * @return True if the methods match given the conditions.
 */
fun Method.softCompareTo(
    otherMethod: MethodReference
): Boolean {
    if (MethodUtil.isConstructor(this) && !parametersEqual(this.parameterTypes, otherMethod.parameterTypes))
        return false
    return this.name == otherMethod.name
}

/**
 * Clones the method.
 * @param registerCount This parameter allows you to change the register count of the method.
 * This may be a positive or negative number.
 * @return The **immutable** cloned method. Call [toMutable] or [cloneMutable] to get a **mutable** copy.
 */
internal fun Method.clone(
    registerCount: Int = 0,
): ImmutableMethod {
    val clonedImplementation = implementation?.let {
        ImmutableMethodImplementation(
            it.registerCount + registerCount,
            it.instructions,
            it.tryBlocks,
            it.debugItems,
        )
    }
    return ImmutableMethod(
        returnType,
        name,
        parameters,
        returnType,
        accessFlags,
        annotations,
        hiddenApiRestrictions,
        clonedImplementation
    )
}

/**
 * Add smali instructions to the method.
 * @param index The index to insert the instructions at.
 * @param instruction The smali instruction to add.
 */
fun MutableMethod.addInstruction(index: Int, instruction: String) =
    this.implementation!!.addInstruction(index, instruction.toInstruction(this))

/**
 * Add smali instructions to the method.
 * @param index The index to insert the instructions at.
 * @param instructions The smali instructions to add.
 */
fun MutableMethod.addInstructions(index: Int, instructions: String) =
    this.implementation!!.addInstructions(index, instructions.toInstructions(this))

/**
 * Clones the method.
 * @param registerCount This parameter allows you to change the register count of the method.
 * This may be a positive or negative number.
 * @return The **mutable** cloned method. Call [clone] to get an **immutable** copy.
 */
internal fun Method.cloneMutable(
    registerCount: Int = 0,
) = clone(registerCount).toMutable()

// FIXME: also check the order of parameters as different order equals different method overload
internal fun parametersEqual(
    parameters1: Iterable<CharSequence>,
    parameters2: Iterable<CharSequence>
): Boolean {
    return parameters1.count() == parameters2.count() && parameters1.all { parameter ->
        parameters2.any {
            it.startsWith(
                parameter
            )
        }
    }
}

internal val nullOutputStream: OutputStream =
    object : OutputStream() {
        override fun write(b: Int) {}
    }