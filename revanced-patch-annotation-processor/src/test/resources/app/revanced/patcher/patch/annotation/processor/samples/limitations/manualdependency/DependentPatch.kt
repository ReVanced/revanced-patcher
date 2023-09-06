package app.revanced.patcher.patch.annotation.processor.samples.limitations.manualdependency
import app.revanced.patcher.data.BytecodeContext
import app.revanced.patcher.patch.BytecodePatch
import app.revanced.patcher.patch.annotation.Patch

@Patch(name = "Dependent patch")
object DependentPatch : BytecodePatch(
    // Dependency will not be executed correctly if it is manually specified.
    // The reason for this is that the dependency patch is annotated too,
    // so the processor will generate a new patch class for it embedding the annotated information.
    // Because the dependency is manually specified,
    // the processor will not be able to change this dependency to the generated class,
    // which means that the dependency will lose the annotated information.
    dependencies = setOf(DependencyPatch::class)
) {
    override fun execute(context: BytecodeContext) {}
}