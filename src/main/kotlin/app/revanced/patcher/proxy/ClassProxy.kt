package app.revanced.patcher.proxy

import app.revanced.patcher.proxy.mutableTypes.MutableClass
import org.jf.dexlib2.iface.ClassDef


class ClassProxy(
    val immutableClass: ClassDef,
    val originalClassIndex: Int,
) {
    internal var proxyUsed = false
    internal lateinit var mutatedClass: MutableClass

    fun resolve(): MutableClass {
        if (!proxyUsed) {
            proxyUsed = true
            mutatedClass = MutableClass(immutableClass)
        }
        return mutatedClass
    }
}