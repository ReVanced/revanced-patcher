package app.revanced.patcher.patch

import app.revanced.patcher.usage.bytecode.ExampleBytecodePatch
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

internal class PatchOptionsTest {
    private val options = ExampleBytecodePatch().options

    @Test
    fun `should not throw an exception`() {
        for (option in options) {
            when (option) {
                is PatchOption.StringOption -> {
                    option.value = "Hello World"
                }
                is PatchOption.BooleanOption -> {
                    option.value = false
                }
                is PatchOption.StringListOption -> {
                    option.value = option.options.first()
                    for (choice in option.options) {
                        println(choice)
                    }
                }
                is PatchOption.IntListOption -> {
                    option.value = option.options.first()
                    for (choice in option.options) {
                        println(choice)
                    }
                }
            }
        }
        println(options["key1"].value)
        options["key1"] = "Hello, world!"
        println(options["key1"].value)
    }

    @Test
    fun `should be able to set value to null`() {
        // Sadly, doing:
        // > options["key2"] = null
        // is not possible because Kotlin
        // cannot reify the type "Nothing?".
        options.nullify("key2")
    }

    @Test
    fun `should fail because the option does not exist`() {
        assertThrows<NoSuchOptionException> {
            options["this option does not exist"] = 123
        }
    }

    @Test
    fun `should fail because of invalid value type`() {
        assertThrows<InvalidTypeException> {
            options["key1"] = 123
        }
    }

    @Test
    fun `should fail because of an illegal value`() {
        assertThrows<IllegalValueException> {
            options["key3"] = "this value is not an allowed option"
        }
    }

    @Test
    fun `should fail because of the requirement is not met`() {
        assertThrows<RequirementNotMetException> {
            options.nullify("key1")
        }
    }
}