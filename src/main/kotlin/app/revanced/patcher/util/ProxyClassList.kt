package app.revanced.patcher.util

import app.revanced.patcher.util.proxy.ClassProxy
import com.android.tools.smali.dexlib2.iface.ClassDef

/**
 * A list of classes and proxies.
 *
 * @param classes The classes to be backed by proxies.
 */
class ProxyClassList internal constructor(classes: MutableList<ClassDef>) : MutableList<ClassDef> by classes {
    internal val proxyPool = mutableListOf<ClassProxy>()

    internal fun proxy(classDef: ClassDef) =
        proxyPool.find {
            it.immutableClass.type == classDef.type
        } ?: ClassProxy(classDef).also {
            proxyPool.add(it)

            val index = indexOf(classDef)
            if (index < 0) throw IllegalStateException("Could not find original class index")
            set(index, it.mutableClass)
        }
}
