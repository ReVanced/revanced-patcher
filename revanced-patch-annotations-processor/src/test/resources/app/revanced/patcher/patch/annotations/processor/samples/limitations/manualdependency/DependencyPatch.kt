package app.revanced.patcher.patch.annotations.processor.samples.limitations.manualdependency

import app.revanced.patcher.data.ResourceContext
import app.revanced.patcher.patch.ResourcePatch
import app.revanced.patcher.patch.annotations.Patch

@Patch(name = "Dependency patch")
object DependencyPatch : ResourcePatch() {
    override fun execute(context: ResourceContext) { }
}