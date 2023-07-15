package app.revanced.patcher.util

internal class ListBackedSet<E>(private val list: MutableList<E>) : MutableSet<E> {
    override val size get() = list.size
    override fun add(element: E) = list.add(element)
    override fun addAll(elements: Collection<E>) = list.addAll(elements)
    override fun clear() = list.clear()
    override fun iterator() = list.listIterator()
    override fun remove(element: E) = list.remove(element)
    override fun removeAll(elements: Collection<E>) = list.removeAll(elements)
    override fun retainAll(elements: Collection<E>) = list.retainAll(elements)
    override fun contains(element: E) = list.contains(element)
    override fun containsAll(elements: Collection<E>) = list.containsAll(elements)
    override fun isEmpty() = list.isEmpty()
}