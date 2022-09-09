package app.revanced.patcher.patch

import app.revanced.patcher.usage.bytecode.ExampleBytecodePatch
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import kotlin.test.assertNotEquals

internal class PatchOptionsTest {
    private val options = ExampleBytecodePatch.options

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
        val option = options.get<String>("key1")
        // or: val option: String? by options["key1"]
        // then you won't need `.value` every time
        println(option.value)
        options["key1"] = "Hello, world!"
        println(option.value)
    }

    @Test
    fun `should return a different value when changed`() {
        var value: String? by options["key1"]
        val current = value + "" // force a copy
        value = "Hello, world!"
        assertNotEquals(current, value)
    }

    @Test
    fun `should be able to set value to null`() {
        // Sadly, doing:
        // > options["key2"] = null
        // is not possible because Kotlin
        // cannot reify the type "Nothing?".
        // So we have to do this instead:
        options["key2"] = null as Any?
        // This is a cleaner replacement for the above:
        options.nullify("key2")
    }

    @Test
    fun `should fail because the option does not exist`() {
        assertThrows<NoSuchOptionException> {
            options["this option does not exist"] = 123
        }
    }

    @Test
    fun `should fail because of invalid value type when setting an option`() {
        assertThrows<InvalidTypeException> {
            options["key1"] = 123
        }
    }

    @Test
    fun `should fail because of invalid value type when getting an option`() {
        assertThrows<InvalidTypeException> {
            options.get<Int>("key1")
        }
    }

    @Test
    fun `should fail because of an illegal value`() {
        assertThrows<IllegalValueException> {
            options["key3"] = "this value is not an allowed option"
        }
    }

    @Test
    fun `should fail because the requirement is not met`() {
        assertThrows<RequirementNotMetException> {
            options.nullify("key1")
        }
    }

    @Test
    fun `should fail because getting a non-initialized option is illegal`() {
        assertThrows<RequirementNotMetException> {
            println(options["key5"].value)
        }
    }
}