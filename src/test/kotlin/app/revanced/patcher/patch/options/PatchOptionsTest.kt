package app.revanced.patcher.patch.options

import app.revanced.patcher.data.BytecodeContext
import app.revanced.patcher.patch.BytecodePatch
import app.revanced.patcher.patch.options.PatchOption.PatchExtensions.booleanPatchOption
import app.revanced.patcher.patch.options.PatchOption.PatchExtensions.stringArrayPatchOption
import app.revanced.patcher.patch.options.PatchOption.PatchExtensions.stringPatchOption
import org.junit.jupiter.api.assertDoesNotThrow
import org.junit.jupiter.api.assertThrows
import kotlin.test.Test
import kotlin.test.assertTrue

internal class PatchOptionsTest {
    @Test
    fun `should not fail because default value is unvalidated`() {
        assertDoesNotThrow {
            OptionsTestPatch.options["required"].value
        }
    }

    @Test
    fun `should throw due to incorrect type`() {
        assertThrows<PatchOptionException.InvalidValueTypeException> {
            OptionsTestPatch.options["bool"] = 0
        }
    }

    @Test
    fun `should be nullable`() {
        OptionsTestPatch.options["bool"] = null
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
            value = values.last()
            assertTrue(value == "valid")
        }

    @Test
    fun `should allow setting custom value`() {
        assertDoesNotThrow {
            OptionsTestPatch.options["choices"] = "unknown"
        }
    }

    @Test
    fun `should not allow setting custom value with validation`() =
        @Suppress("UNCHECKED_CAST")
        with(OptionsTestPatch.options["validated"] as PatchOption<String>) {
            // Getter validation
            assertThrows<PatchOptionException.ValueValidationException> { value }

            // setter validation on incorrect value
            assertThrows<PatchOptionException.ValueValidationException> { value = "invalid" }

            // Setter validation on correct value
            assertDoesNotThrow { value = "valid" }
        }

    @Suppress("unused")
    private object OptionsTestPatch : BytecodePatch() {
        private var stringOption by stringPatchOption("string", "default")
        private var booleanOption by booleanPatchOption("bool", true)
        private var requiredStringOption by stringPatchOption("required", "default", required = true)
        private var nullDefaultRequiredOption by stringPatchOption("null", null, required = true)

        val stringArrayOption = stringArrayPatchOption("array", arrayOf("1", "2"))
        val stringOptionWithChoices = stringPatchOption("choices", "value", values = setOf("valid"))

        val validatedOption = stringPatchOption("validated", "default") { it == "valid" }

        override fun execute(context: BytecodeContext) {}
    }
}