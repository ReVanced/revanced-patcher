package app.revanced.patcher.util

import org.w3c.dom.Document
import java.io.Closeable
import java.io.File
import java.io.InputStream
import java.io.OutputStream
import javax.xml.parsers.DocumentBuilderFactory
import javax.xml.transform.TransformerFactory
import javax.xml.transform.dom.DOMSource
import javax.xml.transform.stream.StreamResult

/**
 * Wrapper for a file that can be edited as a dom document.
 *
 * This constructor does not check for locks to the file when writing.
 * Use the secondary constructor.
 *
 * @param inputStream the input stream to read the xml file from.
 * @param outputStream the output stream to write the xml file to. If null, the file will be read only.
 *
 */
class DomFileEditor internal constructor(
    private val inputStream: InputStream,
    private val outputStream: Lazy<OutputStream>? = null,
) : Closeable {
    // path to the xml file to unlock the resource when closing the editor
    private var filePath: String? = null
    private var closed: Boolean = false

    /**
     * The document of the xml file
     */
    val file: Document = DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(inputStream)
        .also(Document::normalize)


    // lazily open an output stream
    // this is required because when constructing a DomFileEditor the output stream is created along with the input stream, which is not allowed
    // the workaround is to lazily create the output stream. This way it would be used after the input stream is closed, which happens in the constructor
    constructor(file: File) : this(file.inputStream(), lazy { file.outputStream() }) {
        // increase the lock
        locks.merge(file.path, 1, Integer::sum)
        filePath = file.path
    }

    /**
     * Closes the editor. Write backs and decreases the lock count.
     *
     * Will not write back to the file if the file is still locked.
     */
    override fun close() {
        if (closed) return

        inputStream.close()

        // if the output stream is not null, do not close it
        outputStream?.let {
            // prevent writing to same file, if it is being locked
            // isLocked will be false if the editor was created through a stream
            val isLocked = filePath?.let { path ->
                val isLocked = locks[path]!! > 1
                // decrease the lock count if the editor was opened for a file
                locks.merge(path, -1, Integer::sum)
                isLocked
            } ?: false

            // if unlocked, write back to the file
            if (!isLocked) {
                it.value.use { stream ->
                    val result = StreamResult(stream)
                    TransformerFactory.newInstance().newTransformer().transform(DOMSource(file), result)
                }

                it.value.close()
                return
            }
        }

        closed = true
    }

    private companion object {
        // map of concurrent open files
        val locks = mutableMapOf<String, Int>()
    }
}