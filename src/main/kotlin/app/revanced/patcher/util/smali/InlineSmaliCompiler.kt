package app.revanced.patcher.util.smali

import org.antlr.runtime.CommonTokenStream
import org.antlr.runtime.TokenSource
import org.antlr.runtime.tree.CommonTreeNodeStream
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
         * p0, p1 etc. will only work correctly if the parameters and registers are passed.
         * Do not cross the boundaries of the control flow (if-nez insn, etc),
         * as that will result in exceptions since the labels cannot be calculated.
         * Do not create dummy labels to fix the issue, since the code addresses will
         * be messed up and results in broken Dalvik bytecode.
         * FIXME: Fix the above issue. When this is fixed, add the proper conversions in [InstructionConverter].
         */
        fun compileMethodInstructions(
            instructions: String,
            parameters: String,
            registers: Int,
            forStaticMethod: Boolean
        ): List<BuilderInstruction> {
            val input = METHOD_TEMPLATE.format(if (forStaticMethod) "static" else "", parameters, registers, instructions)
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
            return classDef.methods.first().implementation!!.instructions.map { it.toBuilderInstruction() }
        }
    }
}

fun String.toInstructions(parametersCount: Int = 0, registers: Int = 1, forStaticMethod: Boolean = true) =
    InlineSmaliCompiler.compileMethodInstructions(this, "I".repeat(parametersCount), registers, forStaticMethod)

fun String.toInstruction(parametersCount: Int = 0, registers: Int = 1, forStaticMethod: Boolean = true) =
    this.toInstructions(parametersCount, registers, forStaticMethod).first()
