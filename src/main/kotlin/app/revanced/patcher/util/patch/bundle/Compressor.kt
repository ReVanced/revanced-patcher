package app.revanced.patcher.util.patch.bundle

import java.io.ByteArrayOutputStream
import java.util.zip.Deflater
import java.util.zip.Inflater

private fun deinflate(size: Int, finished: () -> Boolean, call: (ByteArray) -> Int): ByteArray {
    return ByteArrayOutputStream(size).use { stream ->
        val buffer = ByteArray(1024)
        while (!finished()) {
            val count = call(buffer)
            stream.write(buffer, 0, count)
        }
        stream.toByteArray()
    }
}

internal fun Deflater.compress(bytesIn: ByteArray): ByteArray {
    reset()
    setInput(bytesIn)
    finish()
    return deinflate(bytesIn.size, ::finished, ::deflate)
}

internal fun Inflater.decompress(bytesIn: ByteArray): ByteArray {
    reset()
    setInput(bytesIn)
    return deinflate(bytesIn.size, ::finished, ::inflate)
}