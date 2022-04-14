package app.revanced.patcher

import app.revanced.patcher.patch.Patch
import app.revanced.patcher.proxy.ClassProxy
import app.revanced.patcher.signature.SignatureResolverResult
import org.jf.dexlib2.iface.ClassDef

class PatcherData(
    internal val classes: MutableList<ClassDef>,
) {
    internal val classProxies = mutableSetOf<ClassProxy>()
    internal val patches = mutableSetOf<Patch>()

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

                if (predicate(result.definingClassProxy.immutableClass))
                    return result.definingClassProxy  // ...then return that proxy
            }
        }

        // else search the original class list
        val (foundClass, index) = classes.findIndexed(predicate) ?: return null
        // create a class proxy with the index of the class in the classes list
        val classProxy = ClassProxy(foundClass, index)
        // add it to the cache and
        this.classProxies.add(classProxy)
        // return the proxy class
        return classProxy
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

internal inline fun <T> Iterable<T>.findIndexed(predicate: (T) -> Boolean): Pair<T, Int>? {
    for ((index, element) in this.withIndex()) {
        if (predicate(element)) {
            return element to index
        }
    }
    return null
}
