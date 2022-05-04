package app.revanced.patcher.usage

import app.revanced.patcher.data.implementation.ResourceData
import app.revanced.patcher.patch.implementation.ResourcePatch
import app.revanced.patcher.patch.implementation.metadata.PatchMetadata
import app.revanced.patcher.patch.implementation.misc.PatchResult
import app.revanced.patcher.patch.implementation.misc.PatchResultSuccess
import com.sun.org.apache.xerces.internal.dom.ElementImpl

class ExampleResourcePatch : ResourcePatch(
    PatchMetadata(
        "example-patch",
        "Example Resource Patch",
        "Example demonstration of a resource patch.",
        packageMetadata,
        "0.0.1"
    )
) {
    override fun execute(data: ResourceData): PatchResult {
        val editor = data.getXmlEditor("AndroidManifest.xml")

        // regular DomFileEditor
        val element = editor
            .file
            .getElementsByTagName("application")
            .item(0) as ElementImpl
        element
            .setAttribute(
                "exampleAttribute",
                "exampleValue"
            )

        // close the editor to write changes
        editor.close()

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