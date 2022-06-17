package app.revanced.patcher.data.implementation

import app.revanced.patcher.data.base.Data
import org.w3c.dom.Document
import java.io.Closeable
import java.io.File
import javax.xml.XMLConstants
import javax.xml.parsers.DocumentBuilderFactory
import javax.xml.transform.TransformerFactory
import javax.xml.transform.dom.DOMSource
import javax.xml.transform.stream.StreamResult

class ResourceData(private val resourceCacheDirectory: File) : Data {
    private fun resolve(path: String) = resourceCacheDirectory.resolve(path)

    fun forEach(action: (File) -> Unit) = resourceCacheDirectory.walkTopDown().forEach(action)
    fun get(path: String) = resolve(path)

    fun replace(path: String, oldValue: String, newValue: String, oldValueIsRegex: Boolean = false) {
        // TODO: buffer this somehow
        val content = resolve(path).readText()

        if (oldValueIsRegex) {
            content.replace(Regex(oldValue), newValue)
            return
        }
    }

    fun getXmlEditor(path: String) = DomFileEditor(resolve(path))
}

class DomFileEditor internal constructor(private val domFile: File) : Closeable {
    val file: Document

    init {
        val factory = DocumentBuilderFactory.newInstance()

        val builder = factory.newDocumentBuilder()

        // this will expectedly throw
        file = builder.parse(domFile)
        file.normalize()
    }

    override fun close() = TransformerFactory.newInstance().newTransformer()
        .transform(DOMSource(file), StreamResult(domFile.outputStream()))
}
