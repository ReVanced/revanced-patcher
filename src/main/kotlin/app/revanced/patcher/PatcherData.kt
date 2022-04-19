package app.revanced.patcher

import app.revanced.patcher.methodWalker.MethodWalker
import app.revanced.patcher.patch.Patch
import app.revanced.patcher.proxy.ClassProxy
import app.revanced.patcher.signature.SignatureResolverResult
import app.revanced.patcher.util.ProxyBackedClassList
import org.jf.dexlib2.iface.ClassDef
import org.jf.dexlib2.iface.Method

class PatcherData(
    internalClasses: MutableList<ClassDef>,
) {
    val classes = ProxyBackedClassList(internalClasses)
    internal val patches = mutableListOf<Patch>()

    /**
     * Find a class by a given class name
     * @return A proxy for the first class that matches the class name
     */
    fun findClass(className: String) = findClass { it.type.contains(className) }

    /**
     * Find a class by a given predicate
     * @return A proxy for the first class that matches the predicate
     */
    fun findClass(predicate: (ClassDef) -> Boolean): ClassProxy? {
        // if we already proxied the class matching the predicate...
        for (patch in patches) {
            for (signature in patch.signatures) {
                val result = signature.result
                result ?: continue

                if (predicate(result.definingClassProxy.immutableClass)) return result.definingClassProxy  // ...then return that proxy
            }
        }

        // else resolve the class to a proxy and return it, if the predicate is matching a class
        return classes.find(predicate)?.let {
            proxy(it)
        }
    }
}

class MethodMap : LinkedHashMap<String, SignatureResolverResult>() {
    override fun get(key: String): SignatureResolverResult {
        return super.get(key) ?: throw MethodNotFoundException("Method $key was not found in the method cache")
    }
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

fun PatcherData.toMethodWalker(startMethod: Method): MethodWalker {
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

fun PatcherData.proxy(classDef: ClassDef): ClassProxy {
    var proxy = this.classes.proxies.find { it.immutableClass.type == classDef.type }
    if (proxy == null) {
        proxy = ClassProxy(classDef)
        this.classes.proxies.add(proxy)
    }
    return proxy
}