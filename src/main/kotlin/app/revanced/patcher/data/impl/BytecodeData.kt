package app.revanced.patcher.data.impl

import app.revanced.patcher.data.Data
import app.revanced.patcher.util.ProxyBackedClassList
import app.revanced.patcher.util.method.MethodWalker
import org.jf.dexlib2.iface.ClassDef
import org.jf.dexlib2.iface.Method

class BytecodeData(
    internalClasses: MutableList<ClassDef>
) : Data {
    val classes = ProxyBackedClassList(internalClasses)

    /**
     * Find a class by a given class name.
     * @param className The name of the class.
     * @return A proxy for the first class that matches the class name.
     */
    fun findClass(className: String) = findClass { it.type.contains(className) }

    /**
     * Find a class by a given predicate.
     * @param predicate A predicate to match the class.
     * @return A proxy for the first class that matches the predicate.
     */
    fun findClass(predicate: (ClassDef) -> Boolean) =
        // if we already proxied the class matching the predicate...
        classes.proxies.firstOrNull { predicate(it.immutableClass) } ?:
        // else resolve the class to a proxy and return it, if the predicate is matching a class
        classes.find(predicate)?.let { proxy(it) }
}

internal class MethodNotFoundException(s: String) : Exception(s)

internal inline fun <reified T> Iterable<T>.find(predicate: (T) -> Boolean): T? {
    for (element in this) {
        if (predicate(element)) {
            return element
        }
    }
    return null
}

/**
 * Create a [MethodWalker] instance for the current [BytecodeData].
 * @param startMethod The method to start at.
 * @return A [MethodWalker] instance.
 */
fun BytecodeData.toMethodWalker(startMethod: Method): MethodWalker {
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

fun BytecodeData.proxy(classDef: ClassDef): app.revanced.patcher.util.proxy.ClassProxy {
    var proxy = this.classes.proxies.find { it.immutableClass.type == classDef.type }
    if (proxy == null) {
        proxy = app.revanced.patcher.util.proxy.ClassProxy(classDef)
        this.classes.add(proxy)
    }
    return proxy
}