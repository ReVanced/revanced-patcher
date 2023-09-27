package app.revanced.patcher.patch.annotation.processor.samples.dependencies
import app.revanced.patcher.data.BytecodeContext
import app.revanced.patcher.patch.BytecodePatch
import app.revanced.patcher.patch.annotation.Patch

@Patch(
    name = "Dependent patch",
    dependencies = [DependencyPatch::class],
)
object DependentPatch : BytecodePatch() {
    override fun execute(context: BytecodeContext) {}
}