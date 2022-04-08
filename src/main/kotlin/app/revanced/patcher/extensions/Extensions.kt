package app.revanced.patcher.extensions

import org.jf.dexlib2.AccessFlags
import org.jf.dexlib2.builder.BuilderInstruction
import org.jf.dexlib2.builder.MutableMethodImplementation
import org.jf.dexlib2.iface.ClassDef

infix fun AccessFlags.or(other: AccessFlags) = this.value or other.value

fun MutableMethodImplementation.addInstructions(index: Int, instructions: List<BuilderInstruction>) {
    for (i in instructions.lastIndex downTo 0) {
        this.addInstruction(index, instructions[i])
    }
}

internal fun MutableSet<ClassDef>.replace(originalIndex: Int, mutatedClass: ClassDef) {
    this.remove(this.elementAt(originalIndex))
    this.add(mutatedClass)
}
