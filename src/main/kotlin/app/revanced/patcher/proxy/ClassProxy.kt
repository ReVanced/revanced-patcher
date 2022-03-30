package app.revanced.patcher.proxy

import app.revanced.patcher.proxy.mutableTypes.MutableClass
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