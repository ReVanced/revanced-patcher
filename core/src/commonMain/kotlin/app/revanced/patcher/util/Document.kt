package app.revanced.patcher.util

import collections.merge
import com.google.common.base.Charsets
import org.w3c.dom.Document
import java.io.Closeable
import java.io.File
import java.io.InputStream
import java.io.bufferedWriter
import java.io.inputStream
import javax.xml.parsers.DocumentBuilderFactory
import javax.xml.transform.OutputKeys
import javax.xml.transform.TransformerFactory
import javax.xml.transform.dom.DOMSource
import javax.xml.transform.stream.StreamResult
import kotlin.use

class Document internal constructor(
    inputStream: InputStream,
) : Document by DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(inputStream), Closeable {
    private var file: File? = null

    init {
        normalize()
    }

    internal constructor(file: File) : this(file.inputStream()) {
        this.file = file
        readerCount.merge(file, 1, Int::plus)
    }

    override fun close() {
        file?.let {
            if (readerCount[it]!! > 1) {
                throw IllegalStateException(
                    "Two or more instances are currently reading $it." +
                            "To be able to close this instance, no other instances may be reading $it at the same time.",
                )
            } else {
                readerCount.remove(it)
            }

            val transformer = TransformerFactory.newInstance().newTransformer()
            // Set to UTF-16 to prevent surrogate pairs from being escaped to invalid numeric character references, but save as UTF-8.
            if (isAndroid) {
                transformer.setOutputProperty(OutputKeys.ENCODING, "UTF-16")
                transformer.setOutputProperty(OutputKeys.OMIT_XML_DECLARATION, "yes")
                val writer = it.bufferedWriter(charset = Charsets.UTF_8)
                transformer.transform(DOMSource(this), StreamResult(writer))
                writer.close()
            } else {
                transformer.transform(DOMSource(this), StreamResult(it))
            }
        }
    }

    private companion object {
        private val readerCount = mutableMapOf<File, Int>()
        private val isAndroid = System.getProperty("java.runtime.name") == "Android Runtime"
    }
}
