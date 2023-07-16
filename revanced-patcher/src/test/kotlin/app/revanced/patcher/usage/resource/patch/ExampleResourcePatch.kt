package app.revanced.patcher.usage.resource.patch

import app.revanced.patcher.ResourceContext
import app.revanced.patcher.annotation.Description
import app.revanced.patcher.annotation.Name
import app.revanced.patcher.annotation.Version
import app.revanced.patcher.apk.Apk
import app.revanced.patcher.openXmlFile
import app.revanced.patcher.patch.ResourcePatch
import app.revanced.patcher.patch.annotations.Patch
import app.revanced.patcher.usage.resource.annotation.ExampleResourceCompatibility
import org.w3c.dom.Element

@Patch
@Name("example-resource-patch")
@Description("Example demonstration of a resource patch.")
@ExampleResourceCompatibility
@Version("0.0.1")
class ExampleResourcePatch : ResourcePatch {
    override suspend fun execute(context: ResourceContext) {
        context.apkBundle.base.resources.openXmlFile(Apk.MANIFEST_FILE_NAME).use { editor ->
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