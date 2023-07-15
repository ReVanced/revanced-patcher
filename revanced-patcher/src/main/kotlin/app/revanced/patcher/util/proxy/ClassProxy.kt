package app.revanced.patcher.util.proxy

import app.revanced.patcher.util.proxy.mutableTypes.MutableClass
import org.jf.dexlib2.iface.ClassDef

/**
 * A proxy class for a [ClassDef].
 *
 * A class proxy simply holds a reference to the original class
 * and allocates a mutable clone for the original class if needed.
 * @param immutableClass The class to proxy.
 */
class ClassProxy internal constructor(
    val immutableClass: ClassDef,
) {
    /**
     * Weather the proxy was actually used.
     */
    internal var resolved = false

    /**
     * The mutable clone of the original class.
     *
     * Note: This is only allocated if the proxy is actually used.
     */
    val mutableClass by lazy {
        resolved = true
        if (immutableClass is MutableClass) {
            immutableClass
        } else
            MutableClass(immutableClass)
    }
}