package app.revanced.patcher.patch.usage

import app.revanced.patcher.data.ResourceContext
import app.revanced.patcher.patch.ResourcePatch
import org.w3c.dom.Element

class ExampleResourcePatch : ResourcePatch() {
    override fun execute(context: ResourceContext) {
        context.document["AndroidManifest.xml"].use { document ->
            val element = document.getElementsByTagName("application").item(0) as Element
            element.setAttribute("exampleAttribute", "exampleValue")
        }
    }
}
