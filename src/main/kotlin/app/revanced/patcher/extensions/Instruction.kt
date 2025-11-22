package app.revanced.patcher.extensions

import com.android.tools.smali.dexlib2.Opcode
import com.android.tools.smali.dexlib2.iface.instruction.DualReferenceInstruction
import com.android.tools.smali.dexlib2.iface.instruction.Instruction
import com.android.tools.smali.dexlib2.iface.instruction.ReferenceInstruction
import com.android.tools.smali.dexlib2.iface.instruction.WideLiteralInstruction
import com.android.tools.smali.dexlib2.iface.reference.*
import com.android.tools.smali.dexlib2.util.MethodUtil

@Suppress("UNCHECKED_CAST")
fun <T : Reference> Instruction.reference(predicate: T.() -> Boolean) =
    ((this as? ReferenceInstruction)?.reference as? T)?.predicate() ?: false

@Suppress("UNCHECKED_CAST")
fun <T : Reference> Instruction.reference2(predicate: T.() -> Boolean) =
    ((this as? DualReferenceInstruction)?.reference2 as? T)?.predicate() ?: false

fun Instruction.methodReference(predicate: MethodReference.() -> Boolean) =
    reference(predicate)

fun Instruction.methodReference(methodReference: MethodReference) =
    methodReference { MethodUtil.methodSignaturesMatch(methodReference, this) }

fun Instruction.fieldReference(predicate: FieldReference.() -> Boolean) =
    reference(predicate)

fun Instruction.fieldReference(fieldName: String) =
    fieldReference { name == fieldName }

fun Instruction.type(predicate: String.() -> Boolean) =
    reference<TypeReference> { type.predicate() }

fun Instruction.type(type: String) =
    type { this == type }

fun Instruction.string(predicate: String.() -> Boolean) =
    reference<StringReference> { string.predicate() }

fun Instruction.string(string: String) =
    string { this == string }

fun Instruction.opcode(opcode: Opcode) = this.opcode == opcode

fun Instruction.wideLiteral(wideLiteral: Long) = (this as? WideLiteralInstruction)?.wideLiteral == wideLiteral

private inline fun <reified T : Reference> Instruction.reference(): T? =
    (this as? ReferenceInstruction)?.reference as? T

val Instruction.reference: Reference?
    get() = reference()

val Instruction.methodReference get() =
    reference<MethodReference>()

val Instruction.fieldReference get() =
    reference<FieldReference>()

val Instruction.typeReference get() =
    reference<TypeReference>()

val Instruction.stringReference get() =
    reference<StringReference>()

val Instruction.wideLiteral get() =
    (this as? WideLiteralInstruction)?.wideLiteral
