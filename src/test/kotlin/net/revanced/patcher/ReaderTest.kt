package net.revanced.patcher

import kotlin.test.Test

internal class ReaderTest {
    @Test
    fun `read jar containing multiple classes`() {
        val testData = PatcherTest::class.java.getResourceAsStream("/test2.jar")!!
        Patcher(testData, PatcherTest.testSigs) // reusing test sigs from PatcherTest
        testData.close()
    }
}