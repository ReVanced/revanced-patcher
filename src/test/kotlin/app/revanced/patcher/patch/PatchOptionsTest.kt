package app.revanced.patcher.patch

import app.revanced.patcher.extensions.PatchExtensions.options
import app.revanced.patcher.patch.options.OptionsContainer
import app.revanced.patcher.usage.bytecode.ExampleBytecodePatch
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

internal class PatchOptionsTest {
    @Test
    fun `should work`() {
        var opt: String by ExampleBytecodePatch.option("key1")
        assertEquals(opt, "default")
        opt = "test"
        assertEquals(opt, "test")
    }

    @Test
    fun `extension works`() {
        assertNotNull(ExampleBytecodePatch::class.java.options)
        assertEquals(ExampleBytecodePatch as OptionsContainer, ExampleBytecodePatch::class.java.options)
    }

    @Test
    fun `first option should have correct key`() {
        val options = ExampleBytecodePatch.options()
        assertNotNull(options)
        assert(options.isNotEmpty())
        assertEquals(options.first().key, "key1")
    }
}