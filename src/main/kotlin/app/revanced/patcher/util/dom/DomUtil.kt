package app.revanced.patcher.util.dom

import org.w3c.dom.Node

object DomUtil {
    /**
     * Recursively execute the [callback] on the [Node].
     *
     * @param callback The callback with the [Node].
     */
    fun Node.doRecursively(callback: (Node) -> Unit) {
        callback(this)
        for (i in this.childNodes.length - 1 downTo 0) this.childNodes.item(i).doRecursively(callback)
    }
}