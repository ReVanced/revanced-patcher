package app.revanced.patcher.extensions

import app.revanced.patcher.util.proxy.mutableTypes.MutableMethod.Companion.toMutable
import org.jf.dexlib2.AccessFlags
import org.jf.dexlib2.builder.BuilderInstruction
import org.jf.dexlib2.builder.MutableMethodImplementation
import org.jf.dexlib2.iface.Method
import org.jf.dexlib2.iface.reference.MethodReference
import org.jf.dexlib2.immutable.ImmutableMethod
import org.jf.dexlib2.immutable.ImmutableMethodImplementation
import org.jf.dexlib2.util.MethodUtil

/**
 * Recursively find a given annotation on a class
 * @param targetAnnotation The annotation to find
 * @return The annotation
 */
fun <T : Annotation> Class<*>.findAnnotationRecursively(targetAnnotation: Class<T>) =
    this.findAnnotationRecursively(targetAnnotation, mutableSetOf())

private fun <T : Annotation> Class<*>.findAnnotationRecursively(
    targetAnnotation: Class<T>,
    traversed: MutableSet<Annotation>
): T? {
    val found = this.annotations.firstOrNull { it.annotationClass.java.name == targetAnnotation.name }

    @Suppress("UNCHECKED_CAST")
    if (found != null) return found as T

    for (annotation in this.annotations) {
        if (traversed.contains(annotation)) continue
        traversed.add(annotation)

        return (annotation.annotationClass.java.findAnnotationRecursively(targetAnnotation, traversed)) ?: continue
    }

    return null
}

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
 * Clones the method.
 * @param registerCount This parameter allows you to change the register count of the method.
 * This may be a positive or negative number.
 * @return The **mutable** cloned method. Call [clone] to get an **immutable** copy.
 */
internal fun Method.cloneMutable(
    registerCount: Int = 0,
) = clone(registerCount).toMutable()

internal fun Method.softCompareTo(
    otherMethod: MethodReference
): Boolean {
    if (MethodUtil.isConstructor(this) && !parametersEqual(this.parameterTypes, otherMethod.parameterTypes))
        return false
    return this.name == otherMethod.name
}

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