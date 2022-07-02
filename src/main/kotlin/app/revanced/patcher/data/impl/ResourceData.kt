package app.revanced.patcher.data.impl

import app.revanced.patcher.data.Data
import org.w3c.dom.Document
import java.io.Closeable
import java.io.File
import javax.xml.parsers.DocumentBuilderFactory
import javax.xml.transform.TransformerFactory
import javax.xml.transform.dom.DOMSource
import javax.xml.transform.stream.StreamResult

class ResourceData(private val resourceCacheDirectory: File) : Data, Iterable<File> {
    operator fun get(path: String) = resourceCacheDirectory.resolve(path)
    val xmlEditor = XmlFileHolder()
    override fun iterator() = resourceCacheDirectory.walkTopDown().iterator()

    inner class XmlFileHolder {
        operator fun get(path: String) = DomFileEditor(this@ResourceData[path])
    }
}

class DomFileEditor internal constructor(private val domFile: File) : Closeable {
    val file: Document = DocumentBuilderFactory.newInstance().newDocumentBuilder()
        .parse(domFile).also(Document::normalize)

    override fun close() = TransformerFactory.newInstance().newTransformer()
        .transform(DOMSource(file), StreamResult(domFile.outputStream()))
}
