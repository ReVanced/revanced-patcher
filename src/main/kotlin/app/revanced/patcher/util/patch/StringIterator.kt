package app.revanced.patcher.util.patch

internal class StringIterator<T, I : Iterator<T>>(
    private val iterator: I,
    private val _next: (T) -> String
) : Iterator<String> {
    override fun hasNext() = iterator.hasNext()
    override fun next() = _next(iterator.next())
}