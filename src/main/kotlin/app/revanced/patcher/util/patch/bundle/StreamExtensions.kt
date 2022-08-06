package app.revanced.patcher.util.patch.bundle

import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream

internal fun ByteArrayOutputStream.writeString(str: String) {
    write(str.length)
    write(str.toByteArray())
}

internal fun ByteArrayInputStream.readString() = String(readNBytesAssert(readChecked()))

internal fun ByteArrayInputStream.readChecked() = read(::checkSize)

internal fun ByteArrayInputStream.readChecked(validator: (Int) -> Boolean) = read { checkSize(it) && validator(it) }

internal fun ByteArrayInputStream.read(validator: (Int) -> Boolean) = read().also {
    if (!validator(it)) throw IllegalValueException
}

internal fun ByteArrayInputStream.readNBytesAssert(len: Int) = readNBytes(len).also {
    if (it.size != len) throw EOFReachedException(len)
}

internal fun ByteArrayInputStream.readNBytesAssert(len: Int, validator: (ByteArray) -> Boolean) =
    readNBytesAssert(len).also {
        if (!validator(it)) throw IllegalValueException
    }

private fun checkSize(size: Int): Boolean {
    if (size > Int.MAX_VALUE) throw IllegalStateException("Size is stupidly high, malicious bundle?")
    if (size < 0) throw MalformedBundleException("Malformed size")
    return true
}