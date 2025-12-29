package java.io

internal expect fun File.kmpResolve(child: String): File

internal fun File.resolve(child: String) = kmpResolve(child)

internal expect fun File.kmpDeleteRecursively(): Boolean

internal fun File.deleteRecursively() = kmpDeleteRecursively()

internal expect fun File.kmpInputStream(): InputStream

internal fun File.inputStream() = kmpInputStream()

internal expect fun File.kmpBufferedWriter(charset: java.nio.charset.Charset): BufferedWriter

internal fun File.bufferedWriter(charset: java.nio.charset.Charset) = kmpBufferedWriter(charset)