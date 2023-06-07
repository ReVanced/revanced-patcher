package app.revanced.patcher.extensions

import app.revanced.patcher.util.proxy.mutableTypes.MutableMethod
import app.revanced.patcher.util.proxy.mutableTypes.MutableMethod.Companion.toMutable
import app.revanced.patcher.util.smali.ExternalLabel
import org.jf.dexlib2.AccessFlags
import org.jf.dexlib2.Opcode
import org.jf.dexlib2.builder.BuilderOffsetInstruction
import org.jf.dexlib2.builder.MutableMethodImplementation
import org.jf.dexlib2.builder.instruction.BuilderInstruction21s
import org.jf.dexlib2.iface.instruction.OneRegisterInstruction
import org.jf.dexlib2.immutable.ImmutableMethod
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
    fun addInstructionsToImplementationIndexed() = applyOnImplementation {
        addInstructions(5, getTestInstructions(5..6)).also {
            assertRegisterIs(5, 5)
            assertRegisterIs(6, 6)

            assertRegisterIs(5, 7)
        }
    }

    @Test
    fun addInstructionsToImplementation() = applyOnImplementation {
        addInstructions(getTestInstructions(10..11)).also {
            assertRegisterIs(10, 10)
            assertRegisterIs(11, 11)
        }
    }

    @Test
    fun removeInstructionsFromImplementationIndexed() = applyOnImplementation {
        removeInstructions(5, 5).also { assertRegisterIs(4, 4) }
    }

    @Test
    fun removeInstructionsFromImplementation() = applyOnImplementation {
        removeInstructions(0).also { assertRegisterIs(9, 9) }
        removeInstructions(1).also { assertRegisterIs(1, 0) }
        removeInstructions(2).also { assertRegisterIs(3, 0) }
    }

    @Test
    fun replaceInstructionsInImplementationIndexed() = applyOnImplementation {
        replaceInstructions(5, getTestInstructions(0..1)).also {
            assertRegisterIs(0, 5)
            assertRegisterIs(1, 6)
            assertRegisterIs(7, 7)
        }
    }

    @Test
    fun addInstructionToMethodIndexed() = applyOnMethod {
        addInstruction(5, TestInstruction(0)).also { assertRegisterIs(0, 5) }
    }

    @Test
    fun addInstructionToMethod() = applyOnMethod {
        addInstruction(TestInstruction(0)).also { assertRegisterIs(0, 10) }
    }

    @Test
    fun addSmaliInstructionToMethodIndexed() = applyOnMethod {
        addInstruction(5, getTestSmaliInstruction(0)).also { assertRegisterIs(0, 5) }
    }

    @Test
    fun addSmaliInstructionToMethod() = applyOnMethod {
        addInstruction(getTestSmaliInstruction(0)).also { assertRegisterIs(0, 10) }
    }

    @Test
    fun addInstructionsToMethodIndexed() = applyOnMethod {
        addInstructions(5, getTestInstructions(0..1)).also {
            assertRegisterIs(0, 5)
            assertRegisterIs(1, 6)

            assertRegisterIs(5, 7)
        }
    }

    @Test
    fun addInstructionsToMethod() = applyOnMethod {
        addInstructions(getTestInstructions(0..1)).also {
            assertRegisterIs(0, 10)
            assertRegisterIs(1, 11)

            assertRegisterIs(9, 9)
        }
    }

    @Test
    fun addSmaliInstructionsToMethodIndexed() = applyOnMethod {
        addInstructionsWithLabels(5, getTestSmaliInstructions(0..1)).also {
            assertRegisterIs(0, 5)
            assertRegisterIs(1, 6)

            assertRegisterIs(5, 7)
        }
    }

    @Test
    fun addSmaliInstructionsToMethod() = applyOnMethod {
        addInstructions(getTestSmaliInstructions(0..1)).also {
            assertRegisterIs(0, 10)
            assertRegisterIs(1, 11)

            assertRegisterIs(9, 9)
        }
    }

    @Test
    fun addSmaliInstructionsWithExternalLabelToMethodIndexed() = applyOnMethod {
        val label = ExternalLabel("testLabel", instruction(5))

        addInstructionsWithLabels(
            5,
            getTestSmaliInstructions(0..1).plus("\n").plus("goto :${label.name}"),
            label
        ).also {
            assertRegisterIs(0, 5)
            assertRegisterIs(1, 6)
            assertRegisterIs(5, 8)

            val gotoTarget = instruction<BuilderOffsetInstruction>(7)
                .target.location.instruction as OneRegisterInstruction

            assertEquals(5, gotoTarget.registerA)
        }
    }

    @Test
    fun removeInstructionFromMethodIndexed() = applyOnMethod {
        removeInstruction(5).also {
            assertRegisterIs(4, 4)
            assertRegisterIs(6, 5)
        }
    }

    @Test
    fun removeInstructionsFromMethodIndexed() = applyOnMethod {
        removeInstructions(5, 5).also { assertRegisterIs(4, 4) }
    }

    @Test
    fun removeInstructionsFromMethod() = applyOnMethod {
        removeInstructions(0).also { assertRegisterIs(9, 9) }
        removeInstructions(1).also { assertRegisterIs(1, 0) }
        removeInstructions(2).also { assertRegisterIs(3, 0) }
    }

    @Test
    fun replaceInstructionInMethodIndexed() = applyOnMethod {
        replaceInstruction(5, TestInstruction(0)).also { assertRegisterIs(0, 5) }
    }

    @Test
    fun replaceInstructionsInMethodIndexed() = applyOnMethod {
        replaceInstructions(5, getTestInstructions(0..1)).also {
            assertRegisterIs(0, 5)
            assertRegisterIs(1, 6)
            assertRegisterIs(7, 7)
        }
    }

    @Test
    fun replaceSmaliInstructionsInMethodIndexed() = applyOnMethod {
        replaceInstructions(5, getTestSmaliInstructions(0..1)).also {
            assertRegisterIs(0, 5)
            assertRegisterIs(1, 6)
            assertRegisterIs(7, 7)
        }
    }

    // region Helper methods

    private fun applyOnImplementation(block: MutableMethodImplementation.() -> Unit) {
        testMethodImplementation.apply(block)
    }

    private fun applyOnMethod(block: MutableMethod.() -> Unit) {
        testMethod.apply(block)
    }

    private fun MutableMethodImplementation.assertRegisterIs(register: Int, atIndex: Int) = assertEquals(
        register, instruction<OneRegisterInstruction>(atIndex).registerA
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