package app.revanced.patcher.util

import org.w3c.dom.Document
import java.io.Closeable
import java.io.File
import java.io.InputStream
import java.io.StringWriter
import javax.xml.parsers.DocumentBuilderFactory
import javax.xml.transform.TransformerFactory
import javax.xml.transform.dom.DOMSource
import javax.xml.transform.stream.StreamResult

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

    private fun fixSurrogatePairs(xml: String): String {
        val regex = """&#(\d+);&#(\d+);""".toRegex()
        return regex.replace(xml) { match ->
            val high = match.groupValues[1].toInt()
            val low = match.groupValues[2].toInt()

            // Convert surrogate pairs into a single Unicode code point
            if (Character.isSurrogatePair(high.toChar(), low.toChar())) {
                val codePoint = Character.toCodePoint(high.toChar(), low.toChar())
                "&#$codePoint;"
            } else {
                match.value // Fallback (should not happen)
            }
        }
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

            val writer = StringWriter()

            TransformerFactory.newInstance()
                .newTransformer()
                .transform(DOMSource(this), StreamResult(writer))

            val fixedXml = fixSurrogatePairs(writer.toString())

            it.outputStream().use { stream ->
                stream.write(fixedXml.toByteArray(Charsets.UTF_8))
            }
        }
    }

    private companion object {
        private val readerCount = mutableMapOf<File, Int>()
    }
}
