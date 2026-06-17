package app.revanced.patcher.patch

import java.io.File
import java.nio.file.Files
import java.util.Locale
import java.util.logging.Logger
import java.util.stream.Collectors

internal object ResourceFileSanitizer {
    private val pngMagic = byteArrayOf(0x89.toByte(), 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A)
    private val jpegMagic = byteArrayOf(0xFF.toByte(), 0xD8.toByte(), 0xFF.toByte())

    fun sanitizePngResources(
        resDirectory: File,
        logger: Logger,
    ): List<Result> {
        if (!resDirectory.exists()) return emptyList()

        val paths = Files.walk(resDirectory.toPath())
        return try {
            paths
                .filter(Files::isRegularFile)
                .map { it.toFile() }
                .filter { it.name.lowercase(Locale.ROOT).endsWith(".png") }
                .map { file -> sanitizePngResource(resDirectory, file, logger) }
                .filter { it.action != Action.PRESERVED }
                .collect(Collectors.toList())
        } finally {
            paths.close()
        }
    }

    fun detectFormat(file: File): Format {
        val header = ByteArray(16)
        val readBytes =
            file.inputStream().use { inputStream ->
                inputStream.read(header)
            }.coerceAtLeast(0)

        val bytes = header.copyOf(readBytes)

        return when {
            bytes.startsWith(pngMagic) -> Format.PNG
            bytes.startsWith(jpegMagic) -> Format.JPEG
            bytes.isWebP() -> Format.WEBP
            bytes.isIsoBmff("avif") || bytes.isIsoBmff("avis") -> Format.AVIF
            bytes.isIsoBmff("heic") || bytes.isIsoBmff("heix") || bytes.isIsoBmff("hevc") || bytes.isIsoBmff("hevx") ->
                Format.HEIF
            else -> Format.UNKNOWN
        }
    }

    private fun sanitizePngResource(
        resDirectory: File,
        file: File,
        logger: Logger,
    ): Result {
        val format = detectFormat(file)
        if (format == Format.PNG) return Result(file, format, Action.PRESERVED)

        val relativePath = file.relativeTo(resDirectory).path
        if (file.name.lowercase(Locale.ROOT).endsWith(".9.png")) {
            throw PatchException(
                "Resource sanitization failed for $relativePath. Detected format: $format. " +
                    "Nine-patch resources must remain valid PNG files and cannot be renamed safely. " +
                    "Replace the file with a valid .9.png resource.",
            )
        }

        val extension =
            format.resourceExtension ?: throw PatchException(
                "Resource sanitization failed for $relativePath. Detected format: $format. " +
                    "The file has a .png extension but is not a PNG image, and the real format could not be detected. " +
                    "Replace it with a valid PNG or a supported Android drawable format.",
            )

        val renamedFile = file.resolveSibling(file.nameWithoutExtension + extension)
        if (renamedFile.exists()) {
            throw PatchException(
                "Resource sanitization failed for $relativePath. Detected format: $format. " +
                    "Cannot rename to ${renamedFile.name} because that file already exists. " +
                    "Remove the duplicate resource or replace $relativePath with a valid PNG.",
            )
        }

        Files.move(file.toPath(), renamedFile.toPath())
        logger.warning(
            "Sanitized resource ${file.relativeTo(resDirectory)}: detected $format content in a .png file; " +
                "renamed to ${renamedFile.relativeTo(resDirectory)} before resource compilation.",
        )

        return Result(renamedFile, format, Action.RENAMED)
    }

    private fun ByteArray.startsWith(prefix: ByteArray) =
        size >= prefix.size && prefix.indices.all { index -> this[index] == prefix[index] }

    private fun ByteArray.isWebP() =
        size >= 12 &&
            this[0] == 'R'.code.toByte() &&
            this[1] == 'I'.code.toByte() &&
            this[2] == 'F'.code.toByte() &&
            this[3] == 'F'.code.toByte() &&
            this[8] == 'W'.code.toByte() &&
            this[9] == 'E'.code.toByte() &&
            this[10] == 'B'.code.toByte() &&
            this[11] == 'P'.code.toByte()

    private fun ByteArray.isIsoBmff(brand: String) =
        size >= 12 &&
            this[4] == 'f'.code.toByte() &&
            this[5] == 't'.code.toByte() &&
            this[6] == 'y'.code.toByte() &&
            this[7] == 'p'.code.toByte() &&
            this[8] == brand[0].code.toByte() &&
            this[9] == brand[1].code.toByte() &&
            this[10] == brand[2].code.toByte() &&
            this[11] == brand[3].code.toByte()

    enum class Format(
        val resourceExtension: String?,
    ) {
        PNG(".png"),
        JPEG(".jpg"),
        WEBP(".webp"),
        AVIF(".avif"),
        HEIF(null),
        UNKNOWN(null),
    }

    enum class Action {
        PRESERVED,
        RENAMED,
    }

    data class Result(
        val file: File,
        val detectedFormat: Format,
        val action: Action,
    )
}
