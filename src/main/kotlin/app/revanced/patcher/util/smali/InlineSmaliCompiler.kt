package app.revanced.patcher.util.smali

import org.antlr.runtime.CommonTokenStream
import org.antlr.runtime.TokenSource
import org.antlr.runtime.tree.CommonTreeNodeStream
import org.jf.dexlib2.AccessFlags
import org.jf.dexlib2.Opcodes
import org.jf.dexlib2.builder.BuilderInstruction
import org.jf.dexlib2.iface.Method
import org.jf.dexlib2.writer.builder.DexBuilder
import org.jf.smali.LexerErrorInterface
import org.jf.smali.smaliFlexLexer
import org.jf.smali.smaliParser
import org.jf.smali.smaliTreeWalker
import java.io.InputStreamReader

class InlineSmaliCompiler {
    companion object {
        private const val METHOD_TEMPLATE = """
                .class LInlineCompiler;
                .super Ljava/lang/Object;
                .method %s dummyMethod(%s)V
                    .registers %d
                    %s
                .end method
            """

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
            instructions: String, parameters: String, registers: Int, forStaticMethod: Boolean
        ): List<BuilderInstruction> {
            val input =
                METHOD_TEMPLATE.format(if (forStaticMethod) "static" else "", parameters, registers, instructions)
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

/**
 * Compile lines of Smali code to a list of instructions.
 * @param templateMethod The method to compile the instructions against.
 * @returns A list of instructions.
 */
fun String.toInstructions(templateMethod: Method? = null) = InlineSmaliCompiler.compileMethodInstructions(this,
    templateMethod?.parameters?.joinToString("") { it } ?: "",
    templateMethod?.implementation?.registerCount ?: 0,
    (templateMethod?.accessFlags ?: 0) and AccessFlags.STATIC.value != 0)

/**
 * Compile a line of Smali code to an instruction.
 * @param templateMethod The method to compile the instructions against.
 * @return The instruction.
 */
fun String.toInstruction(templateMethod: Method? = null) = this.toInstructions(templateMethod).first()
