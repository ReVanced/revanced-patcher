package app.revanced.patcher.patch.options

import app.revanced.patcher.data.BytecodeContext
import app.revanced.patcher.patch.BytecodePatch
import app.revanced.patcher.patch.options.types.BooleanPatchOption.Companion.booleanPatchOption
import app.revanced.patcher.patch.options.types.StringPatchOption.Companion.stringPatchOption
import app.revanced.patcher.patch.options.types.array.StringArrayPatchOption.Companion.stringArrayPatchOption
import org.junit.jupiter.api.assertDoesNotThrow
import org.junit.jupiter.api.assertThrows
import kotlin.test.Test

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

    private object OptionsTestPatch : BytecodePatch() {
        private var stringOption by stringPatchOption("string", "default")
        private var booleanOption by booleanPatchOption("bool", true)
        private var requiredStringOption by stringPatchOption("required", "default", required = true)
        private var nullDefaultRequiredOption by stringPatchOption("null", null, required = true)

        val stringArrayOption = stringArrayPatchOption("array", arrayOf("1", "2"))

        override fun execute(context: BytecodeContext) {}
    }
}