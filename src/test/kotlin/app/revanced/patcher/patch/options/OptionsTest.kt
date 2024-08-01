package app.revanced.patcher.patch.options

import app.revanced.patcher.patch.*
import org.junit.jupiter.api.assertDoesNotThrow
import org.junit.jupiter.api.assertThrows
import kotlin.reflect.typeOf
import kotlin.test.*

internal object OptionsTest {
    private val externalOption = stringOption("external", "default")

    private val optionsTestPatch = bytecodePatch {
        externalOption()

        booleanOption("bool", true)

        stringOption("required", "default", required = true)

        stringsOption("list", listOf("1", "2"))

        stringOption("choices", "value", values = mapOf("Valid option value" to "valid"))

        stringOption("validated", "default") { it == "valid" }

        stringOption("resettable", null, required = true)
    }

    @Test
    fun `should not fail because default value is unvalidated`() = options {
        assertDoesNotThrow { get("required") }
    }

    @Test
    fun `should not allow setting custom value with validation`() = options {
        // Getter validation on incorrect value.
        assertThrows<OptionException.ValueValidationException> {
            set("validated", get("validated"))
        }

        // Setter validation on incorrect value.
        assertThrows<OptionException.ValueValidationException> {
            set("validated", "invalid")
        }

        // Setter validation on correct value.
        assertDoesNotThrow {
            set("validated", "valid")
        }
    }

    @Test
    fun `should throw due to incorrect type`() = options {
        assertThrows<OptionException.InvalidValueTypeException> {
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
        assertThrows<OptionException.OptionNotFoundException> {
            set("this option does not exist", 1)
        }
    }

    @Test
    fun `should be able to add options manually`() = options {
        assertDoesNotThrow {
            bytecodePatch {
                get("list")()
            }.options["list"]
        }
    }

    @Test
    fun `should allow setting value from values`() = options {
        @Suppress("UNCHECKED_CAST")
        val option = get("choices") as Option<String>

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

        assertThrows<OptionException.ValueRequiredException> {
            get("resettable").value
        }
    }

    @Test
    fun `option types should be known`() = options {
        assertEquals(typeOf<List<String>>(), get("list").type)
    }

    @Test
    fun `getting default value should work`() = options {
        assertDoesNotThrow {
            assertNull(get("resettable").default)
        }
    }

    @Test
    fun `external option should be accessible`() {
        assertDoesNotThrow {
            externalOption.value = "test"
        }
    }

    @Test
    fun `should allow getting the external option from the patch`() {
        assertEquals(optionsTestPatch.options["external"].value, externalOption.value)
    }

    private fun options(block: Options.() -> Unit) = optionsTestPatch.options.let(block)
}
