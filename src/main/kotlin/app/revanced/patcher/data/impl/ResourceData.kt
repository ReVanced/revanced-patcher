package app.revanced.patcher.data.impl

import app.revanced.patcher.data.Data
import org.w3c.dom.Document
import java.io.Closeable
import java.io.File
import javax.xml.parsers.DocumentBuilderFactory
import javax.xml.transform.TransformerFactory
import javax.xml.transform.dom.DOMSource
import javax.xml.transform.stream.StreamResult

class ResourceData(private val resourceCacheDirectory: File) : Data {
    private fun resolve(path: String) = resourceCacheDirectory.resolve(path)

    fun forEach(action: (File) -> Unit) = resourceCacheDirectory.walkTopDown().forEach(action)
    fun get(path: String) = resolve(path)

    fun getXmlEditor(path: String) = DomFileEditor(resolve(path))
}

class DomFileEditor internal constructor(private val domFile: File) : Closeable {
    val file: Document = DocumentBuilderFactory.newInstance().newDocumentBuilder()
        .parse(domFile).also(Document::normalize)

    override fun close() = TransformerFactory.newInstance().newTransformer()
        .transform(DOMSource(file), StreamResult(domFile.outputStream()))
}
