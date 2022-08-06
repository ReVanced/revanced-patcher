package app.revanced.patcher.util.patch.bundle

import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream

internal fun ByteArrayOutputStream.writeString(str: String) {
    write(str.length)
    write(str.toByteArray())
}

internal fun ByteArrayInputStream.readString() = String(readNBytes(read()))

internal fun ByteArrayInputStream.readChecked() = read().also(::checkSize)

private fun checkSize(size: Int) {
    if (size > Int.MAX_VALUE) throw IllegalStateException("Resource size is stupidly high, malicious bundle?")
    if (size < 1) throw MalformedBundleException("Malformed resource size")
}