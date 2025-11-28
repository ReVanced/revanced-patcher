package app.revanced.patcher.extensions

import app.revanced.patcher.dex.mutable.MutableMethod
import com.android.tools.smali.dexlib2.AccessFlags
import com.android.tools.smali.dexlib2.Opcode
import com.android.tools.smali.dexlib2.Opcodes
import com.android.tools.smali.dexlib2.builder.BuilderInstruction
import com.android.tools.smali.dexlib2.iface.instruction.DualReferenceInstruction
import com.android.tools.smali.dexlib2.iface.instruction.Instruction
import com.android.tools.smali.dexlib2.iface.instruction.ReferenceInstruction
import com.android.tools.smali.dexlib2.iface.instruction.WideLiteralInstruction
import com.android.tools.smali.dexlib2.iface.reference.*
import com.android.tools.smali.dexlib2.util.MethodUtil
import com.android.tools.smali.dexlib2.writer.builder.DexBuilder
import com.android.tools.smali.smali.smaliFlexLexer
import com.android.tools.smali.smali.smaliParser
import com.android.tools.smali.smali.smaliTreeWalker
import org.antlr.runtime.CommonTokenStream
import org.antlr.runtime.TokenSource
import org.antlr.runtime.tree.CommonTreeNodeStream
import java.io.StringReader

inline fun <reified T : Reference> Instruction.reference(predicate: T.() -> Boolean) =
    ((this as? ReferenceInstruction)?.reference as? T)?.predicate() ?: false

inline fun <reified T : Reference> Instruction.reference2(predicate: T.() -> Boolean) =
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

val Instruction.methodReference
    get() =
        reference<MethodReference>()

val Instruction.fieldReference
    get() =
        reference<FieldReference>()

val Instruction.typeReference
    get() =
        reference<TypeReference>()

val Instruction.stringReference
    get() =
        reference<StringReference>()

val Instruction.wideLiteral
    get() =
        (this as? WideLiteralInstruction)?.wideLiteral


private const val CLASS_HEADER = ".class LInlineCompiler;\n.super Ljava/lang/Object;\n"
private const val STATIC_HEADER = "$CLASS_HEADER.method public static dummyMethod("
private const val HEADER = "$CLASS_HEADER.method public dummyMethod("
private val sb by lazy { StringBuilder(512) }

/**
 * Compile lines of Smali code to a list of instructions.
 *
 * Note: Adding compiled instructions to an existing method with
 * offset instructions WITHOUT specifying a parent method will not work.
 * @param templateMethod The method to compile the instructions against.
 * @returns A list of instructions.
 */
fun String.toInstructions(templateMethod: MutableMethod? = null): List<BuilderInstruction> {
    val parameters = templateMethod?.parameterTypes?.joinToString("") { it } ?: ""
    val registers = templateMethod?.implementation?.registerCount ?: 1 // TODO: Should this be 0?
    val isStatic = templateMethod?.let { AccessFlags.STATIC.isSet(it.accessFlags) } ?: true

    sb.setLength(0) // reset

    if (isStatic) sb.append(STATIC_HEADER) else sb.append(HEADER)
    sb.append(parameters).append(")V\n")
    sb.append("    .registers ").append(registers).append("\n")
    sb.append(trimIndent()).append("\n")
    sb.append(".end method")

    val reader = StringReader(sb.toString())
    val lexer = smaliFlexLexer(reader, 15)
    val tokens = CommonTokenStream(lexer as TokenSource)
    val parser = smaliParser(tokens)
    val fileTree = parser.smali_file()

    if (lexer.numberOfSyntaxErrors > 0 || parser.numberOfSyntaxErrors > 0) {
        throw IllegalStateException(
            "Lexer errors: ${lexer.numberOfSyntaxErrors}, Parser errors: ${parser.numberOfSyntaxErrors}"
        )
    }

    val treeStream = CommonTreeNodeStream(fileTree.tree).apply {
        tokenStream = tokens
    }

    val walker = smaliTreeWalker(treeStream)
    walker.setDexBuilder(DexBuilder(Opcodes.getDefault()))

    val classDef = walker.smali_file()
    return classDef.methods.first().instructions.map { it as BuilderInstruction }
}
