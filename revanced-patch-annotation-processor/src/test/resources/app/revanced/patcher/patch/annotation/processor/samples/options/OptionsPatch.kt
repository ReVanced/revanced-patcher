package app.revanced.patcher.patch.annotation.processor.samples.options

import app.revanced.patcher.data.ResourceContext
import app.revanced.patcher.patch.ResourcePatch
import app.revanced.patcher.patch.annotation.Patch
import app.revanced.patcher.patch.options.types.StringPatchOption.Companion.stringPatchOption

@Patch(name = "Options patch")
object OptionsPatch : ResourcePatch() {
    override fun execute(context: ResourceContext) {}

    @Suppress("unused")
    private val printOption by stringPatchOption(
        "print",
        null,
        "Print message",
        "The message to print."
    )
}