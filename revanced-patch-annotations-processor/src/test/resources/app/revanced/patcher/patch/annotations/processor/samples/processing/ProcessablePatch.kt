package app.revanced.patcher.patch.annotations.processor.samples.processing

import app.revanced.patcher.data.BytecodeContext
import app.revanced.patcher.patch.BytecodePatch
import app.revanced.patcher.patch.annotations.Patch

@Patch("Processable patch")
object ProcessablePatch : BytecodePatch() {
    override fun execute(context: BytecodeContext) {}
}