package app.revanced.patcher.cache

import app.revanced.patcher.proxy.ClassProxy
import app.revanced.patcher.signature.SignatureResolverResult
import org.jf.dexlib2.iface.ClassDef

class Cache(
    internal val classes: MutableList<ClassDef>,
    val methodMap: MethodMap = MethodMap()
) {
    // TODO: currently we create ClassProxies at multiple places, which is why we could have merge conflicts
    //  this can be solved by creating a dedicated method for creating class proxies,
    //  if the class proxy already exists in the cached proxy list below.
    //  The to-do in the method findClass is related
    internal val classProxy = mutableSetOf<ClassProxy>()

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
        // TODO: find a cleaner way to store all proxied classes.
        //  Currently we have to search the method map as well as the class proxy list which is not elegant

        // if we already proxied the class matching the predicate,
        val proxiedClass = classProxy.find { predicate(it.immutableClass) }
        // return that proxy
        if (proxiedClass != null) return proxiedClass
        // if we already have the class matching the predicate in the method map,
        val result = methodMap.entries.find { predicate(it.value.definingClassProxy.immutableClass) }?.value
        if (result != null) return result.definingClassProxy

        // else search the original class list
        val (foundClass, index) = classes.findIndexed(predicate) ?: return null
        // create a class proxy with the index of the class in the classes list
        val classProxy = ClassProxy(foundClass, index)
        // add it to the cache and
        this.classProxy.add(classProxy)
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

internal inline fun <T> Iterable<T>.find(predicate: (T) -> Boolean): T? {
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
