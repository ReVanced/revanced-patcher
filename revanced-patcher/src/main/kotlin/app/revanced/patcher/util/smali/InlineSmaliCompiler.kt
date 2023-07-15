package app.revanced.patcher.util.smali

import app.revanced.patcher.util.proxy.mutableTypes.MutableMethod
import org.antlr.runtime.CommonTokenStream
import org.antlr.runtime.TokenSource
import org.antlr.runtime.tree.CommonTreeNodeStream
import org.jf.dexlib2.AccessFlags
import org.jf.dexlib2.Opcodes
import org.jf.dexlib2.builder.BuilderInstruction
import org.jf.dexlib2.writer.builder.DexBuilder
import org.jf.smali.LexerErrorInterface
import org.jf.smali.smaliFlexLexer
import org.jf.smali.smaliParser
import org.jf.smali.smaliTreeWalker
import java.io.InputStreamReader

private const val METHOD_TEMPLATE = """
    .class LInlineCompiler;
    .super Ljava/lang/Object;
    .method %s dummyMethod(%s)V
        .registers %d
        %s
    .end method
"""

class InlineSmaliCompiler {
    companion object {
        /**
         * Compiles a string of Smali code to a list of instructions.
         * Special registers (such as p0, p1) will only work correctly
         * if the parameters and registers of the method are passed.
         */
        fun compile(
            instructions: String, parameters: String, registers: Int, forStaticMethod: Boolean
        ): List<BuilderInstruction> {
            val input = METHOD_TEMPLATE.format(
                if (forStaticMethod) {
                    "static"
                } else {
                    ""
                }, parameters, registers, instructions
            )
            val reader = InputStreamReader(input.byteInputStream())
            val lexer: LexerErrorInterface = smaliFlexLexer(reader, 15)
            val tokens = CommonTokenStream(lexer as TokenSource)
            val parser = smaliParser(tokens)
            val result = parser.smali_file()
            if (parser.numberOfSyntaxErrors > 0 || lexer.numberOfSyntaxErrors > 0) {
                throw IllegalStateException(
                    "Encountered ${parser.numberOfSyntaxErrors} parser syntax errors and ${lexer.numberOfSyntaxErrors} lexer syntax errors!"
                )
            }
            val treeStream = CommonTreeNodeStream(result.tree)
            treeStream.tokenStream = tokens
            val dexGen = smaliTreeWalker(treeStream)
            dexGen.setDexBuilder(DexBuilder(Opcodes.getDefault()))
            val classDef = dexGen.smali_file()
            return classDef.methods.first().implementation!!.instructions.map { it as BuilderInstruction }
        }
    }
}

/**
 * Compile lines of Smali code to a list of instructions.
 *
 * Note: Adding compiled instructions to an existing method with
 * offset instructions WITHOUT specifying a parent method will not work.
 * @param method The method to compile the instructions against.
 * @returns A list of instructions.
 */
fun String.toInstructions(method: MutableMethod? = null): List<BuilderInstruction> {
    return InlineSmaliCompiler.compile(this,
        method?.parameters?.joinToString("") { it } ?: "",
        method?.implementation?.registerCount ?: 1,
        method?.let { AccessFlags.STATIC.isSet(it.accessFlags) } ?: true
    )
}

/**
 * Compile a line of Smali code to an instruction.
 * @param templateMethod The method to compile the instructions against.
 * @return The instruction.
 */
fun String.toInstruction(templateMethod: MutableMethod? = null) = this.toInstructions(templateMethod).first()