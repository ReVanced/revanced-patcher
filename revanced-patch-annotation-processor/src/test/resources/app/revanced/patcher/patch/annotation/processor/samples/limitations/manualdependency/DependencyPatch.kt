package app.revanced.patcher.patch.annotation.processor.samples.limitations.manualdependency

import app.revanced.patcher.data.ResourceContext
import app.revanced.patcher.patch.ResourcePatch
import app.revanced.patcher.patch.annotation.Patch

@Patch(name = "Dependency patch")
object DependencyPatch : ResourcePatch() {
    override fun execute(context: ResourceContext) { }
}