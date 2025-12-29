package app.revanced.patcher.util

import app.revanced.patcher.extensions.*
import com.android.tools.smali.dexlib2.AccessFlags
import com.android.tools.smali.dexlib2.builder.BuilderOffsetInstruction
import com.android.tools.smali.dexlib2.builder.MutableMethodImplementation
import com.android.tools.smali.dexlib2.builder.instruction.BuilderInstruction21t
import com.android.tools.smali.dexlib2.immutable.ImmutableMethod
import com.android.tools.smali.dexlib2.mutable.MutableMethod.Companion.toMutable
import org.junit.jupiter.api.BeforeEach
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

internal class SmaliTest {
    val method = ImmutableMethod(
        "Ldummy;",
        "name",
        emptyList(), // parameters
        "V",
        AccessFlags.PUBLIC.value,
        null,
        null,
        MutableMethodImplementation(1),
    ).toMutable()


    @BeforeEach
    fun setup() {
        method.instructions.clear()
    }

    @Test
    fun `own branches work`() {
        method.addInstructionsWithLabels(
            0,
            """
                :test
                const/4 v0, 0x1
                if-eqz v0, :test
            """,
        )

        val targetLocationIndex = method.getInstruction<BuilderOffsetInstruction>(0).target.location.index

        assertEquals(0, targetLocationIndex, "Label should point to index 0")
    }

    @Test
    fun `external branches work`() {
        val instructionIndex = 3
        val labelIndex = 1

        method.addInstructions(
            """
                const/4 v0, 0x1
                const/4 v0, 0x0
            """,
        )

        method.addInstructionsWithLabels(
            method.instructions.size,
            """
                const/4 v0, 0x1
                if-eqz v0, :test
                return-void
            """,
            ExternalLabel("test", method.getInstruction(1)),
        )

        val instruction = method.getInstruction<BuilderInstruction21t>(instructionIndex)

        assertTrue(instruction.target.isPlaced, "Label should be placed")
        assertEquals(labelIndex, instruction.target.location.index)
    }
}