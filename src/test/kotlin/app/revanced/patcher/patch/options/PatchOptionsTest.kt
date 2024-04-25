package app.revanced.patcher.patch.options

import app.revanced.patcher.patch.*
import org.junit.jupiter.api.assertDoesNotThrow
import org.junit.jupiter.api.assertThrows
import kotlin.test.Test
import kotlin.test.assertNull
import kotlin.test.assertTrue

internal class PatchOptionsTest {
    private val optionsTestPatch = bytecodePatch {
        booleanPatchOption("bool", true)

        stringPatchOption("required", "default", required = true)

        stringArrayPatchOption("array", arrayOf("1", "2"))

        stringPatchOption("choices", "value", values = mapOf("Valid option value" to "valid"))

        stringPatchOption("validated", "default") { it == "valid" }

        stringPatchOption("resettable", null, required = true)
    }

    @Test
    fun `should not fail because default value is unvalidated`() = options {
        assertDoesNotThrow { get("required") }
    }

    @Test
    fun `should not allow setting custom value with validation`() = options {
        // Getter validation on incorrect value.
        assertThrows<PatchOptionException.ValueValidationException> {
            set("validated", get("validated"))
        }

        // Setter validation on incorrect value.
        assertThrows<PatchOptionException.ValueValidationException> {
            set("validated", "invalid")
        }

        // Setter validation on correct value.
        assertDoesNotThrow {
            set("validated", "valid")
        }
    }

    @Test
    fun `should throw due to incorrect type`() = options {
        assertThrows<PatchOptionException.InvalidValueTypeException> {
            set("bool", "not a boolean")
        }
    }

    @Test
    fun `should be nullable`() = options {
        assertDoesNotThrow {
            set("bool", null)
        }
    }

    @Test
    fun `option should not be found`() = options {
        assertThrows<PatchOptionException.PatchOptionNotFoundException> {
            set("this option does not exist", 1)
        }
    }

    @Test
    fun `should be able to add options manually`() = options {
        assertDoesNotThrow {
            bytecodePatch {
                option(get("array"))
            }.options["array"]
        }
    }

    @Test
    fun `should allow setting value from values`() = options {
        @Suppress("UNCHECKED_CAST")
        val option = get("choices") as PatchOption<String>

        option.value = option.values!!.values.last()

        assertTrue(option.value == "valid")
    }

    @Test
    fun `should allow setting custom value`() = options {
        assertDoesNotThrow {
            set("choices", "unknown")
        }
    }

    @Test
    fun `should allow resetting value`() = options {
        assertDoesNotThrow {
            set("choices", null)
        }

        assert(get("choices").value == null)
    }

    @Test
    fun `reset should not fail`() = options {
        assertDoesNotThrow {
            set("resettable", "test")
            get("resettable").reset()
        }

        assertThrows<PatchOptionException.ValueRequiredException> {
            get("resettable").value
        }
    }

    @Test
    fun `option types should be known`() = options {
        assertTrue(get("array").valueType == "StringArray")
    }

    @Test
    fun `getting default value should work`() = options {
        assertDoesNotThrow {
            assertNull(get("resettable").default)
        }
    }

    private fun options(block: PatchOptions.() -> Unit) = optionsTestPatch.options.let(block)
}
