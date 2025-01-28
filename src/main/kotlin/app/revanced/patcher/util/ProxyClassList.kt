package app.revanced.patcher.util

import app.revanced.patcher.util.proxy.mutableTypes.MutableClass
import com.android.tools.smali.dexlib2.iface.ClassDef

/**
 * A list of classes and proxies.
 *
 * @param classes The classes to be backed by proxies.
 */
class ProxyClassList internal constructor(classes: MutableList<ClassDef>) : MutableList<ClassDef> by classes {
    internal val proxyPool = mutableListOf<MutableClass>()

    internal fun proxy(classDef: ClassDef) =
        proxyPool.find {
            it.type == classDef.type
        } ?: MutableClass(classDef).also {
            proxyPool.add(it)

            val index = indexOf(classDef)
            if (index < 0) throw IllegalStateException("Could not find original class index")
            set(index, it)
        }

    internal fun proxy(predicate: (ClassDef) -> Boolean) =
        proxyPool.find { predicate(it) } ?: find(predicate)?.let { proxy(it) }
}
