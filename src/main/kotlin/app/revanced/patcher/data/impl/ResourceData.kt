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
            DomFileEditor(inputStream, outputStream)

        operator fun get(path: String) = DomFileEditor(this@ResourceData[path])
    }
}

class DomFileEditor internal constructor(inputStream: InputStream, private val outputStream: OutputStream) : Closeable {
    constructor(file: File) : this(file.inputStream(), file.outputStream())

    val file: Document =
        DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(inputStream).also(Document::normalize)

    override fun close() =
        TransformerFactory.newInstance().newTransformer().transform(DOMSource(file), StreamResult(outputStream))

}
