package app.revanced.patcher.data

import app.revanced.patcher.util.ProxyBackedClassList
import app.revanced.patcher.util.method.MethodWalker
import org.jf.dexlib2.iface.ClassDef
import org.jf.dexlib2.iface.Method
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
 * A common interface to constrain [Context] to [BytecodeContext] and [ResourceContext].
 */

sealed interface Context

class BytecodeContext internal constructor(classes: MutableList<ClassDef>) : Context {
    /**
     * The list of classes.
     */
    val classes = ProxyBackedClassList(classes)

    /**
     * Find a class by a given class name.
     *
     * @param className The name of the class.
     * @return A proxy for the first class that matches the class name.
     */
    fun findClass(className: String) = findClass { it.type.contains(className) }

    /**
     * Find a class by a given predicate.
     *
     * @param predicate A predicate to match the class.
     * @return A proxy for the first class that matches the predicate.
     */
    fun findClass(predicate: (ClassDef) -> Boolean) =
        // if we already proxied the class matching the predicate...
        classes.proxies.firstOrNull { predicate(it.immutableClass) } ?:
        // else resolve the class to a proxy and return it, if the predicate is matching a class
        classes.find(predicate)?.let { proxy(it) }

    fun proxy(classDef: ClassDef): app.revanced.patcher.util.proxy.ClassProxy {
        var proxy = this.classes.proxies.find { it.immutableClass.type == classDef.type }
        if (proxy == null) {
            proxy = app.revanced.patcher.util.proxy.ClassProxy(classDef)
            this.classes.add(proxy)
        }
        return proxy
    }
}

/**
 * Create a [MethodWalker] instance for the current [BytecodeContext].
 *
 * @param startMethod The method to start at.
 * @return A [MethodWalker] instance.
 */
fun BytecodeContext.toMethodWalker(startMethod: Method): MethodWalker {
    return MethodWalker(this, startMethod)
}

internal inline fun <T> Iterable<T>.findIndexed(predicate: (T) -> Boolean): Pair<T, Int>? {
    for ((index, element) in this.withIndex()) {
        if (predicate(element)) {
            return element to index
        }
    }
    return null
}

class ResourceContext internal constructor(private val resourceCacheDirectory: File) : Context, Iterable<File> {
    val xmlEditor = XmlFileHolder()

    operator fun get(path: String) = resourceCacheDirectory.resolve(path)

    override fun iterator() = resourceCacheDirectory.walkTopDown().iterator()

    inner class XmlFileHolder {
        operator fun get(inputStream: InputStream) =
            DomFileEditor(inputStream)

        operator fun get(path: String): DomFileEditor {
            return DomFileEditor(this@ResourceContext[path])
        }

    }
}

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