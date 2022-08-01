package app.revanced.patcher.patch

sealed class PatchOption<T>(
    val key: String,
    default: T,
    val title: String,
    val description: String,
    val required: Boolean = false
) {
    var value: T = default

    class StringOption(
        key: String,
        default: String,
        title: String,
        description: String,
        required: Boolean = false
    ) : PatchOption<String>(
        key, default, title, description, required
    )

    class BooleanOption(
        key: String,
        default: Boolean,
        title: String,
        description: String,
        required: Boolean = false
    ) : PatchOption<Boolean>(
        key, default, title, description, required
    )

    sealed class ListOption<E>(
        key: String,
        default: E,
        val options: Iterable<E>,
        title: String,
        description: String,
        required: Boolean = false
    ) : PatchOption<E>(
        key, default, title, description, required
    ) {
        init {
            if (default !in options) {
                throw IllegalStateException("Default option must be in options list")
            }
        }
    }

    class StringListOption(
        key: String,
        default: String,
        options: Iterable<String>,
        title: String,
        description: String,
        required: Boolean = false
    ) : ListOption<String>(
        key, default, options, title, description, required
    )

    class IntListOption(
        key: String,
        default: Int,
        options: Iterable<Int>,
        title: String,
        description: String,
        required: Boolean = false
    ) : ListOption<Int>(
        key, default, options, title, description, required
    )
}