package app.revanced.patcher.proxy

import app.revanced.patcher.proxy.mutableTypes.MutableClass
import org.jf.dexlib2.iface.ClassDef

/**
 * A proxy class for a [ClassDef].
 *
 * A class proxy simply holds a reference to the original class
 * and allocates a mutable clone for the original class if needed.
 * @param immutableClass The class to proxy
 */
class ClassProxy(
    val immutableClass: ClassDef,
) {
    internal var proxyUsed = false
    internal lateinit var mutatedClass: MutableClass

    init {
        // in the instance, that a [MutableClass] is being proxied,
        // do not create an additional clone and reuse the [MutableClass] instance
        if (immutableClass is MutableClass) {
            mutatedClass = immutableClass
            proxyUsed = true
        }
    }

    /**
     * Allocates and returns a mutable clone of the original class.
     * A patch should always use the original immutable class reference
     * to avoid unnecessary allocations for the mutable class.
     * @return A mutable clone of the original class.
     */
    fun resolve(): MutableClass {
        if (!proxyUsed) {
            proxyUsed = true
            mutatedClass = MutableClass(immutableClass)
        }
        return mutatedClass
    }
}