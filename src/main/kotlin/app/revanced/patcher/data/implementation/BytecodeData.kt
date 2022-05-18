package app.revanced.patcher.data.implementation

import app.revanced.patcher.data.base.Data
import app.revanced.patcher.patch.base.Patch
import app.revanced.patcher.patch.implementation.BytecodePatch
import app.revanced.patcher.signature.implementation.method.resolver.SignatureResolverResult
import app.revanced.patcher.util.ProxyBackedClassList
import app.revanced.patcher.util.method.MethodWalker
import org.jf.dexlib2.iface.ClassDef
import org.jf.dexlib2.iface.Method

class BytecodeData(
    // FIXME: ugly solution due to design.
    //  It does not make sense for a BytecodeData instance to have access to the patches
    private val patches: List<Patch<Data>>,
    internalClasses: MutableList<ClassDef>
) : Data {
    val classes = ProxyBackedClassList(internalClasses)

    /**
     * Find a class by a given class name
     * @return A proxy for the first class that matches the class name
     */
    fun findClass(className: String) = findClass { it.type.contains(className) }

    /**
     * Find a class by a given predicate
     * @return A proxy for the first class that matches the predicate
     */
    fun findClass(predicate: (ClassDef) -> Boolean): app.revanced.patcher.util.proxy.ClassProxy? {
        // if we already proxied the class matching the predicate...
        for (patch in patches) {
            if (patch !is BytecodePatch) continue
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
        this.classes.proxies.add(proxy)
    }
    return proxy
}