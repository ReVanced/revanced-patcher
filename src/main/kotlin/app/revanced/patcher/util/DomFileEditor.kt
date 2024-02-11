package app.revanced.patcher.util

import org.w3c.dom.Document
import java.io.Closeable
import java.io.File
import java.io.InputStream

@Deprecated("Use Document instead.")
class DomFileEditor : Closeable {
    val file: Document
    internal constructor(
        inputStream: InputStream,
    ) {
        file = Document(inputStream)
    }

    constructor(file: File) {
        this.file = Document(file)
    }

    override fun close() {
        file as app.revanced.patcher.util.Document
        file.close()
    }
}
