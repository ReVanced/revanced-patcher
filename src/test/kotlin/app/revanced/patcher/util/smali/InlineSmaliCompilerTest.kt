package app.revanced.patcher.util.smali

import app.revanced.patcher.extensions.InstructionExtensions.addInstructions
import app.revanced.patcher.extensions.InstructionExtensions.addInstructionsWithLabels
import app.revanced.patcher.extensions.InstructionExtensions.getInstruction
import app.revanced.patcher.extensions.newLabel
import app.revanced.patcher.util.proxy.mutableTypes.MutableMethod.Companion.toMutable
import com.android.tools.smali.dexlib2.AccessFlags
import com.android.tools.smali.dexlib2.Opcode
import com.android.tools.smali.dexlib2.builder.BuilderInstruction
import com.android.tools.smali.dexlib2.builder.MutableMethodImplementation
import com.android.tools.smali.dexlib2.builder.instruction.BuilderInstruction21c
import com.android.tools.smali.dexlib2.builder.instruction.BuilderInstruction21t
import com.android.tools.smali.dexlib2.immutable.ImmutableMethod
import com.android.tools.smali.dexlib2.immutable.reference.ImmutableStringReference
import java.util.*
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

internal object InlineSmaliCompilerTest {
    @Test
    fun `outputs valid instruction`() {
        val want = BuilderInstruction21c(Opcode.CONST_STRING, 0, ImmutableStringReference("Test")) as BuilderInstruction
        val have = "const-string v0, \"Test\"".toInstruction()

        assertInstructionsEqual(want, have)
    }

    @Test
    fun `supports branching with own branches`() {
        val method = createMethod()
        val instructionCount = 8
        val instructionIndex = instructionCount - 2
        val targetIndex = instructionIndex - 1

        method.addInstructions(
            arrayOfNulls<String>(instructionCount).also {
                Arrays.fill(it, "const/4 v0, 0x0")
            }.joinToString("\n"),
        )
        method.addInstructionsWithLabels(
            targetIndex,
            """
                :test
                const/4 v0, 0x1
                if-eqz v0, :test
            """,
        )

        val instruction = method.getInstruction<BuilderInstruction21t>(instructionIndex)

        assertEquals(targetIndex, instruction.target.location.index)
    }

    @Test
    fun `supports branching to outside branches`() {
        val method = createMethod()
        val instructionIndex = 3
        val labelIndex = 1

        method.addInstructions(
            """
                const/4 v0, 0x1
                const/4 v0, 0x0
            """,
        )

        assertEquals(labelIndex, method.newLabel(labelIndex).location.index)

        method.addInstructionsWithLabels(
            method.implementation!!.instructions.size,
            """
                const/4 v0, 0x1
                if-eqz v0, :test
                return-void
            """,
            ExternalLabel("test", method.getInstruction(1)),
        )

        val instruction = method.getInstruction<BuilderInstruction21t>(instructionIndex)
        assertTrue(instruction.target.isPlaced, "Label was not placed")
        assertEquals(labelIndex, instruction.target.location.index)
    }

    private fun createMethod(
        name: String = "dummy",
        returnType: String = "V",
        accessFlags: Int = AccessFlags.STATIC.value,
        registerCount: Int = 1,
    ) = ImmutableMethod(
        "Ldummy;",
        name,
        emptyList(), // parameters
        returnType,
        accessFlags,
        emptySet(),
        emptySet(),
        MutableMethodImplementation(registerCount),
    ).toMutable()

    private fun assertInstructionsEqual(want: BuilderInstruction, have: BuilderInstruction) {
        assertEquals(want.opcode, have.opcode)
        assertEquals(want.format, have.format)
        assertEquals(want.codeUnits, have.codeUnits)
    }
}
