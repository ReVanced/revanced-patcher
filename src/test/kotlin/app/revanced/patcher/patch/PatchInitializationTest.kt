package app.revanced.patcher.patch

import kotlin.test.Test

object PatchInitializationTest {
    @Test
    fun `initialize using constructor`() {
        val patch = rawResourcePatch(name = "Resource patch test") { assert(true) }

        assert(patch.name == "Resource patch test")
    }
}
