package app.revanced.patcher.patch.options

import app.revanced.patcher.data.BytecodeContext
import app.revanced.patcher.patch.BytecodePatch
import app.revanced.patcher.patch.options.PatchOption.PatchExtensions.booleanPatchOption
import app.revanced.patcher.patch.options.PatchOption.PatchExtensions.stringArrayPatchOption
import app.revanced.patcher.patch.options.PatchOption.PatchExtensions.stringPatchOption
import org.junit.jupiter.api.assertDoesNotThrow
import org.junit.jupiter.api.assertThrows
import kotlin.test.Test
import kotlin.test.assertNull
import kotlin.test.assertTrue

internal class PatchOptionsTest {
    @Test
    fun `should not fail because default value is unvalidated`() {
        assertDoesNotThrow { OptionsTestPatch.requiredStringOption }
    }

    @Test
    fun `should not allow setting custom value with validation`() {
        // Getter validation on incorrect value.
        assertThrows<PatchOptionException.ValueValidationException> { OptionsTestPatch.validatedOption }

        // Setter validation on incorrect value.
        assertThrows<PatchOptionException.ValueValidationException> { OptionsTestPatch.validatedOption = "invalid" }

        // Setter validation on correct value.
        assertDoesNotThrow { OptionsTestPatch.validatedOption = "valid" }
    }

    @Test
    fun `should throw due to incorrect type`() {
        assertThrows<PatchOptionException.InvalidValueTypeException> {
            OptionsTestPatch.options["bool"] = "not a boolean"
        }
    }

    @Test
    fun `should be nullable`() {
        OptionsTestPatch.booleanOption = null
    }

    @Test
    fun `option should not be found`() {
        assertThrows<PatchOptionException.PatchOptionNotFoundException> {
            OptionsTestPatch.options["this option does not exist"] = 1
        }
    }

    @Test
    fun `should be able to add options manually`() {
        assertThrows<PatchOptionException.InvalidValueTypeException> {
            OptionsTestPatch.options["array"] = OptionsTestPatch.stringArrayOption
        }
        assertDoesNotThrow {
            OptionsTestPatch.options.register(OptionsTestPatch.stringArrayOption)
        }
    }

    @Suppress("UNCHECKED_CAST")
    @Test
    fun `should allow setting value from values`() =
        with(OptionsTestPatch.options["choices"] as PatchOption<String>) {
            value = values!!.values.last()
            assertTrue(value == "valid")
        }

    @Test
    fun `should allow setting custom value`() =
        assertDoesNotThrow { OptionsTestPatch.stringOptionWithChoices = "unknown" }

    @Test
    fun `should allow resetting value`() = assertDoesNotThrow { OptionsTestPatch.stringOptionWithChoices = null }

    @Test
    fun `reset should not fail`() {
        assertDoesNotThrow {
            OptionsTestPatch.resettableOption.value = "test"
            OptionsTestPatch.resettableOption.reset()
        }

        assertThrows<PatchOptionException.ValueRequiredException> {
            OptionsTestPatch.resettableOption.value
        }
    }

    @Test
    fun `getting default value should work`() =
        assertDoesNotThrow { assertNull(OptionsTestPatch.resettableOption.default) }

    private object OptionsTestPatch : BytecodePatch() {
        var booleanOption by booleanPatchOption(
            "bool",
            true
        )
        var requiredStringOption by stringPatchOption(
            "required",
            "default",
            required = true
        )
        var stringArrayOption = stringArrayPatchOption(
            "array",
            arrayOf("1", "2")
        )
        var stringOptionWithChoices by stringPatchOption(
            "choices",
            "value",
            values = mapOf("Valid option value" to "valid")
        )
        var validatedOption by stringPatchOption(

            "validated",
            "default"
        ) { it == "valid" }
        var resettableOption = stringPatchOption(
            "resettable", null,
            required = true
        )

        override fun execute(context: BytecodeContext) {}
    }
}