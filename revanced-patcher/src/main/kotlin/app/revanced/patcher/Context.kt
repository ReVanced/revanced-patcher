package app.revanced.patcher

import app.revanced.arsc.resource.ResourceContainer
import app.revanced.patcher.apk.Apk
import app.revanced.patcher.apk.ApkBundle
import app.revanced.arsc.resource.ResourceFile
import app.revanced.patcher.util.method.MethodWalker
import org.jf.dexlib2.iface.Method
import org.w3c.dom.Document
import java.io.Closeable
import java.io.InputStream
import java.io.StringWriter
import javax.xml.parsers.DocumentBuilderFactory
import javax.xml.transform.TransformerFactory
import javax.xml.transform.dom.DOMSource
import javax.xml.transform.stream.StreamResult

/**
 * A common class to constrain [Context] to [BytecodeContext] and [ResourceContext].
 * @param apkBundle The [ApkBundle] for this context.
 */
sealed class Context(val apkBundle: ApkBundle)

/**
 * A context for the bytecode of an [Apk.Base] file.
 *
 * @param apkBundle The [ApkBundle] for this context.
 */
class BytecodeContext internal constructor(apkBundle: ApkBundle) : Context(apkBundle) {
    /**
     * The list of classes.
     */
    val classes = apkBundle.base.bytecodeData.classes

    /**
     * Create a [MethodWalker] instance for the current [BytecodeContext].
     *
     * @param startMethod The method to start at.
     * @return A [MethodWalker] instance.
     */
    fun traceMethodCalls(startMethod: Method) = MethodWalker(this, startMethod)
}

/**
 * A context for [Apk] file resources.
 *
 * @param apkBundle the [ApkBundle] for this context.
 */
class ResourceContext internal constructor(apkBundle: ApkBundle) : Context(apkBundle) {

    /**
     * Open an [DomFileEditor] for a given DOM file.
     *
     * @param inputStream The input stream to read the DOM file from.
     * @return A [DomFileEditor] instance.
     */
    fun openXmlFile(inputStream: InputStream) = DomFileEditor(inputStream)
}


/**
 * Open a [DomFileEditor] for a resource file in the archive.
 *
 * @see [ResourceContainer.openFile]
 * @param path The resource file path.
 * @return A [DomFileEditor].
 */
fun ResourceContainer.openXmlFile(path: String) = DomFileEditor(openFile(path))

/**
 * Wrapper for a file that can be edited as a dom document.
 *
 * @param inputStream the input stream to read the xml file from.
 * @param onSave A callback that will be called when the editor is closed to save the file.
 */
class DomFileEditor internal constructor(
    private val inputStream: InputStream,
    private val onSave: ((String) -> Unit)? = null
) : Closeable {
    private var closed: Boolean = false

    /**
     * The document of the xml file.
     */
    val file: Document = DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(inputStream)
        .also(Document::normalize)

    internal constructor(file: ResourceFile) : this(
        file.inputStream(),
        {
            file.contents = it.toByteArray()
            file.close()
        }
    )

    /**
     * Closes the editor and writes back to the file.
     */
    override fun close() {
        if (closed) return

        inputStream.close()

        onSave?.let { callback ->
            // Save the updated file.
            val writer = StringWriter()
            TransformerFactory.newInstance().newTransformer().transform(DOMSource(file), StreamResult(writer))
            callback(writer.toString())
        }

        closed = true
    }
}