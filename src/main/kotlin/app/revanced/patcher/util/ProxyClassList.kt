package app.revanced.patcher.util

import app.revanced.patcher.util.proxy.ClassProxy
import com.android.tools.smali.dexlib2.iface.ClassDef
import java.util.*

/**
 * A list of classes and proxies.
 *
 * @param classes The classes to be backed by proxies.
 */
class ProxyClassList internal constructor(
    classes: MutableList<ClassDef>,
) : MutableList<ClassDef> by Collections.synchronizedList(classes) {
    internal val proxyPool = Collections.synchronizedList(mutableListOf<ClassProxy>())

    /**
     * Replace all classes with their mutated versions.
     */
    internal fun replaceClasses() {
        proxyPool.removeIf { proxy ->
            // If the proxy is unused, return false to keep it in the proxies list.
            if (!proxy.resolved) return@removeIf false

            // If it has been used, replace the original class with the mutable class.
            remove(proxy.immutableClass)
            add(proxy.mutableClass)

            // Return true to remove the proxy from the proxies list.
            return@removeIf true
        }
    }
}
