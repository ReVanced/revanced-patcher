package app.revanced.patcher

import java.io.ByteArrayOutputStream
import kotlin.test.Test

internal class ReaderTest {
    @Test
    fun `read jar containing multiple classes`() {
        val testData = PatcherTest::class.java.getResourceAsStream("/test2.jar")!!
        Patcher(testData, ByteArrayOutputStream(), PatcherTest.testSignatures) // reusing test sigs from PatcherTest
    }
}