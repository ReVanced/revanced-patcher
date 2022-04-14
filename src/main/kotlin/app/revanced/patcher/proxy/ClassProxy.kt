package app.revanced.patcher.proxy

import app.revanced.patcher.proxy.mutableTypes.MutableClass
import org.jf.dexlib2.iface.ClassDef

/**
 * A proxy class for a [ClassDef].
 *
 * A class proxy simply holds a reference to the original class
 * and allocates a mutable clone for the original class if needed.
 * @param immutableClass The class to proxy
 * @param originalIndex The original index of the class in the list of classes
 */
class ClassProxy(
    val immutableClass: ClassDef,
    val originalIndex: Int,
) {
    internal var proxyUsed = false
    internal lateinit var mutatedClass: MutableClass

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