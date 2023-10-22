package app.revanced.patcher.patch.usage

import app.revanced.patcher.data.ResourceContext
import app.revanced.patcher.patch.ResourcePatch
import org.w3c.dom.Element


class ExampleResourcePatch : ResourcePatch() {
    override fun execute(context: ResourceContext) {
        context.xmlEditor["AndroidManifest.xml"].use { editor ->
            val element = editor // regular DomFileEditor
                .file
                .getElementsByTagName("application")
                .item(0) as Element
            element
                .setAttribute(
                    "exampleAttribute",
                    "exampleValue"
                )
        }
    }
}