package app.revanced.patcher.patch.annotations.processor.samples.dependencies
import app.revanced.patcher.data.BytecodeContext
import app.revanced.patcher.patch.BytecodePatch
import app.revanced.patcher.patch.annotations.Patch

@Patch(
    name = "Dependent patch",
    dependencies = [DependencyPatch::class],
)
object DependentPatch : BytecodePatch() {
    override fun execute(context: BytecodeContext) {}
}