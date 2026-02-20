package app.revanced.java.io

import java.io.File
import java.nio.charset.Charset

internal actual fun File.kmpResolve(child: String) = resolve(child)

internal actual fun File.kmpDeleteRecursively() = deleteRecursively()

internal actual fun File.kmpInputStream() = inputStream()

internal actual fun File.kmpBufferedWriter(charset: Charset) = bufferedWriter(charset)
