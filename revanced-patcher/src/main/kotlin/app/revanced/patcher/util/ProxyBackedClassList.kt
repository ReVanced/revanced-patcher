package app.revanced.patcher.util

import app.revanced.patcher.util.proxy.ClassProxy
import org.jf.dexlib2.iface.ClassDef

/**
 * A class that represents a list of classes and proxies.
 *
 * @param classes The classes to be backed by proxies.
 */
class ProxyBackedClassList(classes: Set<ClassDef>) : Iterable<ClassDef> {
    // A list for pending proxied classes to be added to the current ProxyBackedClassList instance.
    private val proxiedClasses = mutableListOf<ClassProxy>()
    private val mutableClasses = classes.toMutableList()

    /**
     * Replace the [mutableClasses]es with their proxies.
     */
    internal fun applyProxies() {
        proxiedClasses.removeIf { proxy ->
            // If the proxy is unused, keep it in the proxiedClasses list.
            if (!proxy.resolved) return@removeIf false

            with(mutableClasses) {
                remove(proxy.immutableClass)
                add(proxy.mutableClass)
            }

            return@removeIf true
        }
    }

    /**
     * Replace a [ClassDef] at a given [index].
     *
     * @param index The index of the class to be replaced.
     * @param classDef The new class to replace the old one.
     */
    operator fun set(index: Int, classDef: ClassDef) {
        mutableClasses[index] = classDef
    }

    /**
     * Get a [ClassDef] at a given [index].
     *
     * @param index The index of the class.
     */
    operator fun get(index: Int) = mutableClasses[index]

    /**
     * Iterator for the classes in [ProxyBackedClassList].
     *
     * @return The iterator for the classes.
     */
    override fun iterator() = mutableClasses.iterator()

    /**
     * Proxy a [ClassDef].
     *
     * Note: This creates a [ClassProxy] of the [ClassDef], if not already present.
     *
     * @return A proxy for the given class.
     */
    fun proxy(classDef: ClassDef) = proxiedClasses
        .find { it.immutableClass.type == classDef.type } ?: ClassProxy(classDef).also(proxiedClasses::add)

    /**
     * Add a [ClassDef].
     */
    fun add(classDef: ClassDef) = mutableClasses.add(classDef)

    /**
     * Find a class by a given class name.
     *
     * @param className The name of the class.
     * @return A proxy for the first class that matches the class name.
     */
    fun findClassProxied(className: String) = findClassProxied { it.type.contains(className) }

    /**
     * Find a class by a given predicate.
     *
     * @param predicate A predicate to match the class.
     * @return A proxy for the first class that matches the predicate.
     */
    fun findClassProxied(predicate: (ClassDef) -> Boolean) = this.find(predicate)?.let(::proxy)

    val size get() = mutableClasses.size
}