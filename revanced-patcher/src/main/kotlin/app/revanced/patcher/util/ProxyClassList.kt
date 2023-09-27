package app.revanced.patcher.util

import app.revanced.patcher.util.proxy.ClassProxy
import com.android.tools.smali.dexlib2.iface.ClassDef

/**
 * A class that represents a set of classes and proxies.
 *
 * @param classes The classes to be backed by proxies.
 */
class ProxyClassList internal constructor(classes: MutableSet<ClassDef>) : MutableSet<ClassDef> by classes {
    internal val proxies = mutableListOf<ClassProxy>()

    /**
     * Add a [ClassProxy].
     */
    fun add(classProxy: ClassProxy) = proxies.add(classProxy)

    /**
     * Replace all classes with their mutated versions.
     */
    internal fun replaceClasses() = proxies.removeIf { proxy ->
        // If the proxy is unused, return false to keep it in the proxies list.
            if (!proxy.resolved) return@removeIf false

        // If it has been used, replace the original class with the mutable class.
        remove(proxy.immutableClass)
        add(proxy.mutableClass)

        // Return true to remove the proxy from the proxies list.
            return@removeIf true
        }
}