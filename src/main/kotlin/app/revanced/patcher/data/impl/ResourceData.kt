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
        operator fun get(inputStream: InputStream) =
            DomFileEditor(inputStream)

        operator fun get(path: String): DomFileEditor {
            return DomFileEditor(this@ResourceData[path])
        }

    }
}

/**
 * DomFileEditor is a wrapper for a file that can be edited as a dom document.
 *
 * @param inputStream the input stream to read the xml file from.
 * @param outputStream the output stream to write the xml file to. If null, the file will not be written.
 */
class DomFileEditor internal constructor(
    private val inputStream: InputStream,
    private val outputStream: Lazy<OutputStream>? = null,
) : Closeable {
    // path to the xml file to unlock the resource when closing the editor
    private var filePath: String? = null

    /**
     * The document of the xml file
     */
    val file: Document = DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(inputStream)
        .also(Document::normalize)

    // lazily open an output stream
    // this is required because when constructing a DomFileEditor the output stream is created along with the input stream, which is not allowed
    // the workaround is to lazily create the output stream. This way it would be used after the input stream is closed, which happens in the constructor
    constructor(file: File) : this(file.inputStream(), lazy { file.outputStream() }) {
        filePath = file.path

        // prevent sharing mutability of the same file between multiple instances of DomFileEditor
        if (locks.contains(filePath))
            throw IllegalStateException("Can not create a DomFileEditor for that file because it is already locked by another instance of DomFileEditor.")
        locks.add(filePath!!)
    }

    override fun close() {
        inputStream.close()

        // if the output stream is not null, do not close it
        outputStream?.let {
            val result = StreamResult(it.value)
            TransformerFactory.newInstance().newTransformer().transform(DOMSource(file), result)

            it.value.close()
        }

        // remove the lock, if it exists
        filePath?.let {
            locks.remove(it)
        }
    }

    private companion object {
        // list of locked file paths
        val locks = mutableListOf<String>()
    }
}
