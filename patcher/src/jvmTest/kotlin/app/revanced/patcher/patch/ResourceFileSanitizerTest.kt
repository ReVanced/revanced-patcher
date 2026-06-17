package app.revanced.patcher.patch

import org.junit.jupiter.api.assertThrows
import java.nio.file.Files
import java.util.logging.Logger
import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

internal class ResourceFileSanitizerTest {
    private val logger = Logger.getLogger(ResourceFileSanitizerTest::class.java.name)

    @Test
    fun `detects fake png file formats`() {
        withTempResourceDirectory { resDirectory ->
            val jpeg = resDirectory.resolve("drawable/fake_jpeg.png")
            val webp = resDirectory.resolve("drawable/fake_webp.png")
            val avif = resDirectory.resolve("drawable/fake_avif.png")
            val heif = resDirectory.resolve("drawable/fake_heif.png")

            Files.write(jpeg, byteArrayOf(0xFF.toByte(), 0xD8.toByte(), 0xFF.toByte(), 0x00))
            Files.write(webp, byteArrayOf(0x52, 0x49, 0x46, 0x46, 0x00, 0x00, 0x00, 0x00, 0x57, 0x45, 0x42, 0x50))
            Files.write(avif, byteArrayOf(0x00, 0x00, 0x00, 0x18, 0x66, 0x74, 0x79, 0x70, 0x61, 0x76, 0x69, 0x66))
            Files.write(heif, byteArrayOf(0x00, 0x00, 0x00, 0x18, 0x66, 0x74, 0x79, 0x70, 0x68, 0x65, 0x69, 0x63))

            assertEquals(ResourceFileSanitizer.Format.JPEG, ResourceFileSanitizer.detectFormat(jpeg.toFile()))
            assertEquals(ResourceFileSanitizer.Format.WEBP, ResourceFileSanitizer.detectFormat(webp.toFile()))
            assertEquals(ResourceFileSanitizer.Format.AVIF, ResourceFileSanitizer.detectFormat(avif.toFile()))
            assertEquals(ResourceFileSanitizer.Format.HEIF, ResourceFileSanitizer.detectFormat(heif.toFile()))
        }
    }

    @Test
    fun `preserves valid png files`() {
        withTempResourceDirectory { resDirectory ->
            val png = resDirectory.resolve("drawable/icon.png")
            val bytes = byteArrayOf(0x89.toByte(), 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A, 0x00)
            Files.write(png, bytes)

            val results = ResourceFileSanitizer.sanitizePngResources(resDirectory.toFile(), logger)

            assertTrue(results.isEmpty())
            assertTrue(png.toFile().exists())
            assertContentEquals(bytes, png.toFile().readBytes())
        }
    }

    @Test
    fun `preserves valid nine patch png files`() {
        withTempResourceDirectory { resDirectory ->
            val ninePatch = resDirectory.resolve("drawable/button.9.png")
            val bytes = byteArrayOf(0x89.toByte(), 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A, 0x00)
            Files.write(ninePatch, bytes)

            ResourceFileSanitizer.sanitizePngResources(resDirectory.toFile(), logger)

            assertTrue(ninePatch.toFile().exists())
            assertContentEquals(bytes, ninePatch.toFile().readBytes())
        }
    }

    @Test
    fun `rejects fake nine patch png files`() {
        withTempResourceDirectory { resDirectory ->
            val ninePatch = resDirectory.resolve("drawable/button.9.png")
            Files.write(ninePatch, byteArrayOf(0xFF.toByte(), 0xD8.toByte(), 0xFF.toByte(), 0x00))

            val exception =
                assertThrows<PatchException> {
                    ResourceFileSanitizer.sanitizePngResources(resDirectory.toFile(), logger)
                }

            assertTrue(exception.message!!.contains("Nine-patch resources must remain valid PNG files"))
            assertTrue(ninePatch.toFile().exists())
        }
    }

    @Test
    fun `renames fake png resource before compile preserving resource name`() {
        withTempResourceDirectory { resDirectory ->
            val fakePng = resDirectory.resolve("drawable-xxhdpi/bs6.png")
            val bytes = byteArrayOf(0x52, 0x49, 0x46, 0x46, 0x00, 0x00, 0x00, 0x00, 0x57, 0x45, 0x42, 0x50)
            Files.write(fakePng, bytes)

            val results = ResourceFileSanitizer.sanitizePngResources(resDirectory.toFile(), logger)
            val sanitized = resDirectory.resolve("drawable-xxhdpi/bs6.webp")

            assertEquals(1, results.size)
            assertEquals(ResourceFileSanitizer.Format.WEBP, results.single().detectedFormat)
            assertEquals(ResourceFileSanitizer.Action.RENAMED, results.single().action)
            assertFalse(fakePng.toFile().exists())
            assertTrue(sanitized.toFile().exists())
            assertContentEquals(bytes, sanitized.toFile().readBytes())
        }
    }

    private fun withTempResourceDirectory(block: (java.nio.file.Path) -> Unit) {
        val directory = Files.createTempDirectory("resource-sanitizer-test")
        Files.createDirectories(directory.resolve("drawable"))
        Files.createDirectories(directory.resolve("drawable-xxhdpi"))

        try {
            block(directory)
        } finally {
            directory.toFile().deleteRecursively()
        }
    }
}
