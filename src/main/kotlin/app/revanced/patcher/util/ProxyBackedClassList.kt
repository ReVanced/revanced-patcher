package app.revanced.patcher.util

import app.revanced.patcher.proxy.ClassProxy
import org.jf.dexlib2.iface.ClassDef

class ProxyBackedClassList(internal val internalClasses: MutableList<ClassDef>) : List<ClassDef> {
    internal val proxies = mutableListOf<ClassProxy>()

    fun add(classDef: ClassDef) {
        internalClasses.add(classDef)
    }

    fun add(classProxy: ClassProxy) {
        proxies.add(classProxy)
    }

    /**
     * Apply all resolved classes into [internalClasses] and clear the [proxies] list.
     */
    fun applyProxies() {
        proxies.forEachIndexed { i, proxy ->
            if (!proxy.proxyUsed) return@forEachIndexed

            val index = internalClasses.indexOfFirst { it.type == proxy.immutableClass.type }
            internalClasses[index] = proxy.mutatedClass

            proxies.removeAt(i) // FIXME: check if this could cause issues when multiple patches use the same proxy
        }

    }

    override val size get() = internalClasses.size
    override fun contains(element: ClassDef) = internalClasses.contains(element)
    override fun containsAll(elements: Collection<ClassDef>) = internalClasses.containsAll(elements)
    override fun get(index: Int) = internalClasses[index]
    override fun indexOf(element: ClassDef) = internalClasses.indexOf(element)
    override fun isEmpty() = internalClasses.isEmpty()
    override fun iterator() = internalClasses.iterator()
    override fun lastIndexOf(element: ClassDef) = internalClasses.lastIndexOf(element)
    override fun listIterator() = internalClasses.listIterator()
    override fun listIterator(index: Int) = internalClasses.listIterator(index)
    override fun subList(fromIndex: Int, toIndex: Int) = internalClasses.subList(fromIndex, toIndex)
}