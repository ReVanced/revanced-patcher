package app.revanced.patcher.extensions

import app.revanced.patcher.proxy.mutableTypes.MutableMethod
import app.revanced.patcher.proxy.mutableTypes.MutableMethod.Companion.toMutable
import org.jf.dexlib2.AccessFlags
import org.jf.dexlib2.builder.BuilderInstruction
import org.jf.dexlib2.builder.MutableMethodImplementation
import org.jf.dexlib2.iface.Method
import org.jf.dexlib2.immutable.ImmutableMethod
import org.jf.dexlib2.immutable.ImmutableMethodImplementation

infix fun AccessFlags.or(other: AccessFlags) = this.value or other.value
infix fun Int.or(other: AccessFlags) = this or other.value

fun MutableMethodImplementation.addInstructions(index: Int, instructions: List<BuilderInstruction>) {
    for (i in instructions.lastIndex downTo 0) {
        this.addInstruction(index, instructions[i])
    }
}

/**
 * Clones the method.
 * @param registerCount This parameter allows you to change the register count of the method.
 * This may be a positive or negative number.
 * @return The **immutable** cloned method. Call [toMutable] or [cloneMutable] to get a **mutable** copy.
 */
fun Method.clone(
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
 * Clones the method.
 * @param registerCount This parameter allows you to change the register count of the method.
 * This may be a positive or negative number.
 * @return The **mutable** cloned method. Call [clone] to get an **immutable** copy.
 */
fun Method.cloneMutable(
    registerCount: Int = 0,
) = clone(registerCount).toMutable()