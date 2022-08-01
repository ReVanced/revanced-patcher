package app.revanced.patcher.usage

import app.revanced.patcher.patch.PatchOption
import app.revanced.patcher.usage.bytecode.ExampleBytecodePatch

fun patchOptionsUsage() {
    val options = ExampleBytecodePatch().options
    for (opt in options) {
        when (opt) {
            is PatchOption.StringOption -> {
                var option by opt
                println(option)
                option = "Hello World"
                println(option)
            }
            is PatchOption.BooleanOption -> {
                var option by opt
                println(option)
                option = false
                println(option)
            }
        }
    }
}