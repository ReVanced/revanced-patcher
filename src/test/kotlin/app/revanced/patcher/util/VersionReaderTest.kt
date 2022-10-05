package app.revanced.patcher.util

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

internal class VersionReaderTest {
    @Test
    fun read() {
        val version = VersionReader.read()
        assertNotNull(version)
        assertTrue(version.isNotEmpty())
        val parts = version.split(".")
        assertEquals(3, parts.size)
        parts.forEach {
            assertTrue(it.toInt() >= 0)
        }
        println(version)
    }
}