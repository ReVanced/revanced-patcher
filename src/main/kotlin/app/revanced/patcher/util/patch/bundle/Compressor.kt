package app.revanced.patcher.util.patch.bundle

import java.io.ByteArrayOutputStream
import java.util.zip.Deflater
import java.util.zip.Inflater

internal fun Deflater.compress(bytesIn: ByteArray): ByteArray {
    reset()
    setInput(bytesIn)
    finish()
    val bytesOut = ByteArrayOutputStream(bytesIn.size).use { stream ->
        val buffer = ByteArray(1024)
        while (!finished()) {
            val count = deflate(buffer)
            stream.write(buffer, 0, count)
        }
        stream.toByteArray()
    }
    return bytesOut
}

internal fun Inflater.decompress(bytesIn: ByteArray): ByteArray {
    reset()
    setInput(bytesIn)
    val bytesOut = ByteArrayOutputStream(bytesIn.size).use { stream ->
        val buffer = ByteArray(1024)
        while (!finished()) {
            val count = inflate(buffer)
            stream.write(buffer, 0, count)
        }
        stream.toByteArray()
    }
    return bytesOut
}