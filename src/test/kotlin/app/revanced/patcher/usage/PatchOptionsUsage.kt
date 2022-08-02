package app.revanced.patcher.usage

import app.revanced.patcher.patch.PatchOption
import app.revanced.patcher.usage.bytecode.ExampleBytecodePatch
import kotlin.test.Test

internal class PatchOptionsUsage {
    @Test
    fun patchOptionsUsage() {
        val options = ExampleBytecodePatch().options
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
}