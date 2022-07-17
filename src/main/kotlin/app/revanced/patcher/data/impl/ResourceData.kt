package app.revanced.patcher.data.impl

import app.revanced.patcher.data.Data
import org.w3c.dom.Document
import java.io.Closeable
import java.io.File
import java.io.InputStream
import java.io.OutputStream
import javax.xml.parsers.DocumentBuilderFactory
import javax.xml.transform.TransformerFactory
import javax.xml.transform.dom.DOMSource
import javax.xml.transform.stream.StreamResult

class ResourceData(private val resourceCacheDirectory: File) : Data, Iterable<File> {
    val xmlEditor = XmlFileHolder()

    operator fun get(path: String) = resourceCacheDirectory.resolve(path)

    override fun iterator() = resourceCacheDirectory.walkTopDown().iterator()

    inner class XmlFileHolder {
        operator fun get(inputStream: InputStream, outputStream: OutputStream) =
            DomFileEditor(inputStream, lazyOf(outputStream))

        operator fun get(path: String) = DomFileEditor(this@ResourceData[path])
    }
}

class DomFileEditor internal constructor(
    private val inputStream: InputStream,
    private val outputStream: Lazy<OutputStream>,
) : Closeable {
    val file: Document = DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(inputStream)
        .also(Document::normalize)

    // lazily open an output stream
    // this is required because when constructing a DomFileEditor the output stream is created along with the input stream, which is not allowed
    // the workaround is to lazily create the output stream. This way it would be used after the input stream is closed, which happens in the constructor
    constructor(file: File) : this(file.inputStream(), lazy { file.outputStream() })

    override fun close() {
        val result = StreamResult(outputStream.value)
        TransformerFactory.newInstance().newTransformer().transform(DOMSource(file), result)

        inputStream.close()
        outputStream.value.close()
    }

}
