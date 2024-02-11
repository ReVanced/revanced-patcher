package app.revanced.patcher.patch

import app.revanced.patcher.data.ResourceContext
import kotlin.test.Test
import app.revanced.patcher.patch.annotation.Patch as PatchAnnotation

object PatchInitializationTest {
    @Test
    fun `initialize using constructor`() {
        val patch =
            object : RawResourcePatch(name = "Resource patch test") {
                override fun execute(context: ResourceContext) {}
            }

        assert(patch.name == "Resource patch test")
    }

    @Test
    fun `initialize using annotation`() {
        val patch =
            @PatchAnnotation("Resource patch test")
            object : RawResourcePatch() {
                override fun execute(context: ResourceContext) {}
            }

        assert(patch.name == "Resource patch test")
    }
}
