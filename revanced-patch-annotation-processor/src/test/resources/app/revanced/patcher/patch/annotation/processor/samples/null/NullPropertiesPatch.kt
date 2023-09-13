package app.revanced.patcher.patch.annotation.processor.samples.`null`

import app.revanced.patcher.data.BytecodeContext
import app.revanced.patcher.patch.BytecodePatch
import app.revanced.patcher.patch.annotation.CompatiblePackage
import app.revanced.patcher.patch.annotation.Patch

@Patch(
    "Patch with null properties",
    compatiblePackages = [CompatiblePackage("com.google.android.youtube")],
)
object NullPropertiesPatch : BytecodePatch() {
    override fun execute(context: BytecodeContext) {}
}