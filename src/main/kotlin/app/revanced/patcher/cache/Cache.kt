package app.revanced.patcher.cache

import app.revanced.patcher.proxy.ClassProxy
import app.revanced.patcher.signature.SignatureResolverResult
import org.jf.dexlib2.iface.ClassDef

class Cache(
    internal val classes: Set<ClassDef>,
    val resolvedMethods: MethodMap
) {
    // TODO: currently we create ClassProxies at multiple places, which is why we could have merge conflicts
    //  this can be solved by creating a dedicated method for creating class proxies,
    //  if the class proxy already exists in the cached proxy list below
    internal val classProxy = mutableSetOf<ClassProxy>()

    fun findClass(predicate: (ClassDef) -> Boolean): ClassProxy? {
        // if we already proxied the class matching the predicate,
        val proxiedClass = classProxy.singleOrNull { classProxy -> predicate(classProxy.immutableClass) }
        // return that proxy
        if (proxiedClass != null) return proxiedClass
        // else search the original class list
        val foundClass = classes.singleOrNull(predicate) ?: return null
        // create a class proxy with the index of the class in the classes list
        // TODO: There might be a more elegant way to the comment above
        val classProxy = ClassProxy(foundClass, classes.indexOf(foundClass))
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

class MethodNotFoundException(s: String) : Exception(s)
