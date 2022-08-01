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
}