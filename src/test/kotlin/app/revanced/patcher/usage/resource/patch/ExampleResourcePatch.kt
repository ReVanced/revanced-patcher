package app.revanced.patcher.usage.resource.patch

import app.revanced.patcher.annotation.Description
import app.revanced.patcher.annotation.Name
import app.revanced.patcher.annotation.Version
import app.revanced.patcher.data.impl.ResourceData
import app.revanced.patcher.patch.PatchResult
import app.revanced.patcher.patch.PatchResultSuccess
import app.revanced.patcher.patch.annotations.Patch
import app.revanced.patcher.patch.impl.ResourcePatch
import app.revanced.patcher.usage.resource.annotation.ExampleResourceCompatibility
import org.w3c.dom.Element

@Patch
@Name("example-resource-patch")
@Description("Example demonstration of a resource patch.")
@ExampleResourceCompatibility
@Version("0.0.1")
class ExampleResourcePatch : ResourcePatch() {
    override fun execute(data: ResourceData): PatchResult {
        data.getXmlEditor("AndroidManifest.xml").use { domFileEditor ->
            val element = domFileEditor // regular DomFileEditor
                .file
                .getElementsByTagName("application")
                .item(0) as Element
            element
                .setAttribute(
                    "exampleAttribute",
                    "exampleValue"
                )
        }

        // iterate through all available resources
        data.forEach {
            if (it.extension.lowercase() != "xml") return@forEach

            data.replace(
                it.path,
                "\\ddip", // regex supported
                "0dip",
                true
            )
        }

        return PatchResultSuccess()
    }
}