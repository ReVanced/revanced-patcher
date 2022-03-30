package app.revanced.patcher.cache.proxy

import app.revanced.patcher.cache.proxy.mutableTypes.MutableClass
import org.jf.dexlib2.iface.ClassDef


class ClassProxy(
    val immutableClass: ClassDef,
    val originalClassIndex: Int,
) {
    internal var proxyused = false
    internal lateinit var mutatedClass: MutableClass

    fun resolve(): MutableClass {
        if (!proxyused) {
            proxyused = true
            mutatedClass = MutableClass(immutableClass)
        }
        return mutatedClass
    }
}