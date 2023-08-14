package app.revanced.patcher.extensions

import app.revanced.patcher.extensions.InstructionExtensions.addInstruction
import app.revanced.patcher.extensions.InstructionExtensions.addInstructions
import app.revanced.patcher.extensions.InstructionExtensions.addInstructionsWithLabels
import app.revanced.patcher.extensions.InstructionExtensions.getInstruction
import app.revanced.patcher.extensions.InstructionExtensions.removeInstruction
import app.revanced.patcher.extensions.InstructionExtensions.removeInstructions
import app.revanced.patcher.extensions.InstructionExtensions.replaceInstruction
import app.revanced.patcher.extensions.InstructionExtensions.replaceInstructions
import app.revanced.patcher.util.proxy.mutableTypes.MutableMethod
import app.revanced.patcher.util.proxy.mutableTypes.MutableMethod.Companion.toMutable
import app.revanced.patcher.util.smali.ExternalLabel
import com.android.tools.smali.dexlib2.AccessFlags
import com.android.tools.smali.dexlib2.Opcode
import com.android.tools.smali.dexlib2.builder.BuilderOffsetInstruction
import com.android.tools.smali.dexlib2.builder.MutableMethodImplementation
import com.android.tools.smali.dexlib2.builder.instruction.BuilderInstruction21s
import com.android.tools.smali.dexlib2.iface.instruction.OneRegisterInstruction
import com.android.tools.smali.dexlib2.immutable.ImmutableMethod
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals

private object InstructionExtensionsTest {
    private lateinit var testMethod: MutableMethod
    private lateinit var testMethodImplementation: MutableMethodImplementation

    @BeforeEach
    fun createTestMethod() = ImmutableMethod(
        "TestClass;",
        "testMethod",
        null,
        "V",
        AccessFlags.PUBLIC.value,
        null,
        null,
        MutableMethodImplementation(16).also { testMethodImplementation = it }.apply {
            repeat(10) { i -> this.addInstruction(TestInstruction(i)) }
        },
    ).let { testMethod = it.toMutable() }

    @Test
    fun addInstructionsToImplementationIndexed() = applyToImplementation {
        addInstructions(5, getTestInstructions(5..6)).also {
            assertRegisterIs(5, 5)
            assertRegisterIs(6, 6)

            assertRegisterIs(5, 7)
        }
    }

    @Test
    fun addInstructionsToImplementation() = applyToImplementation {
        addInstructions(getTestInstructions(10..11)).also {
            assertRegisterIs(10, 10)
            assertRegisterIs(11, 11)
        }
    }

    @Test
    fun removeInstructionsFromImplementationIndexed() = applyToImplementation {
        removeInstructions(5, 5).also { assertRegisterIs(4, 4) }
    }

    @Test
    fun removeInstructionsFromImplementation() = applyToImplementation {
        removeInstructions(0).also { assertRegisterIs(9, 9) }
        removeInstructions(1).also { assertRegisterIs(1, 0) }
        removeInstructions(2).also { assertRegisterIs(3, 0) }
    }

    @Test
    fun replaceInstructionsInImplementationIndexed() = applyToImplementation {
        replaceInstructions(5, getTestInstructions(0..1)).also {
            assertRegisterIs(0, 5)
            assertRegisterIs(1, 6)
            assertRegisterIs(7, 7)
        }
    }

    @Test
    fun addInstructionToMethodIndexed() = applyToMethod {
        addInstruction(5, TestInstruction(0)).also { assertRegisterIs(0, 5) }
    }

    @Test
    fun addInstructionToMethod() = applyToMethod {
        addInstruction(TestInstruction(0)).also { assertRegisterIs(0, 10) }
    }

    @Test
    fun addSmaliInstructionToMethodIndexed() = applyToMethod {
        addInstruction(5, getTestSmaliInstruction(0)).also { assertRegisterIs(0, 5) }
    }

    @Test
    fun addSmaliInstructionToMethod() = applyToMethod {
        addInstruction(getTestSmaliInstruction(0)).also { assertRegisterIs(0, 10) }
    }

    @Test
    fun addInstructionsToMethodIndexed() = applyToMethod {
        addInstructions(5, getTestInstructions(0..1)).also {
            assertRegisterIs(0, 5)
            assertRegisterIs(1, 6)

            assertRegisterIs(5, 7)
        }
    }

    @Test
    fun addInstructionsToMethod() = applyToMethod {
        addInstructions(getTestInstructions(0..1)).also {
            assertRegisterIs(0, 10)
            assertRegisterIs(1, 11)

            assertRegisterIs(9, 9)
        }
    }

    @Test
    fun addSmaliInstructionsToMethodIndexed() = applyToMethod {
        addInstructionsWithLabels(5, getTestSmaliInstructions(0..1)).also {
            assertRegisterIs(0, 5)
            assertRegisterIs(1, 6)

            assertRegisterIs(5, 7)
        }
    }

    @Test
    fun addSmaliInstructionsToMethod() = applyToMethod {
        addInstructions(getTestSmaliInstructions(0..1)).also {
            assertRegisterIs(0, 10)
            assertRegisterIs(1, 11)

            assertRegisterIs(9, 9)
        }
    }

    @Test
    fun addSmaliInstructionsWithExternalLabelToMethodIndexed() = applyToMethod {
        val label = ExternalLabel("testLabel", getInstruction(5))

        addInstructionsWithLabels(
            5,
            getTestSmaliInstructions(0..1).plus("\n").plus("goto :${label.name}"),
            label
        ).also {
            assertRegisterIs(0, 5)
            assertRegisterIs(1, 6)
            assertRegisterIs(5, 8)

            val gotoTarget = getInstruction<BuilderOffsetInstruction>(7)
                .target.location.instruction as OneRegisterInstruction

            assertEquals(5, gotoTarget.registerA)
        }
    }

    @Test
    fun removeInstructionFromMethodIndexed() = applyToMethod {
        removeInstruction(5).also {
            assertRegisterIs(4, 4)
            assertRegisterIs(6, 5)
        }
    }

    @Test
    fun removeInstructionsFromMethodIndexed() = applyToMethod {
        removeInstructions(5, 5).also { assertRegisterIs(4, 4) }
    }

    @Test
    fun removeInstructionsFromMethod() = applyToMethod {
        removeInstructions(0).also { assertRegisterIs(9, 9) }
        removeInstructions(1).also { assertRegisterIs(1, 0) }
        removeInstructions(2).also { assertRegisterIs(3, 0) }
    }

    @Test
    fun replaceInstructionInMethodIndexed() = applyToMethod {
        replaceInstruction(5, TestInstruction(0)).also { assertRegisterIs(0, 5) }
    }

    @Test
    fun replaceInstructionsInMethodIndexed() = applyToMethod {
        replaceInstructions(5, getTestInstructions(0..1)).also {
            assertRegisterIs(0, 5)
            assertRegisterIs(1, 6)
            assertRegisterIs(7, 7)
        }
    }

    @Test
    fun replaceSmaliInstructionsInMethodIndexed() = applyToMethod {
        replaceInstructions(5, getTestSmaliInstructions(0..1)).also {
            assertRegisterIs(0, 5)
            assertRegisterIs(1, 6)
            assertRegisterIs(7, 7)
        }
    }

    // region Helper methods

    private fun applyToImplementation(block: MutableMethodImplementation.() -> Unit) {
        testMethodImplementation.apply(block)
    }

    private fun applyToMethod(block: MutableMethod.() -> Unit) {
        testMethod.apply(block)
    }

    private fun MutableMethodImplementation.assertRegisterIs(register: Int, atIndex: Int) = assertEquals(
        register, getInstruction<OneRegisterInstruction>(atIndex).registerA
    )

    private fun MutableMethod.assertRegisterIs(register: Int, atIndex: Int) =
        implementation!!.assertRegisterIs(register, atIndex)

    private fun getTestInstructions(range: IntRange) = range.map { TestInstruction(it) }

    private fun getTestSmaliInstruction(register: Int) = "const/16 v$register, 0"

    private fun getTestSmaliInstructions(range: IntRange) = range.joinToString("\n") {
        getTestSmaliInstruction(it)
    }

    // endregion

    private class TestInstruction(register: Int) : BuilderInstruction21s(Opcode.CONST_16, register, 0)
}