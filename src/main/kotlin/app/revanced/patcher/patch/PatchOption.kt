package app.revanced.patcher.patch

/**
 * A [Patch] option.
 * @param key Unique identifier of the option. Example: _`settings.microg.enabled`_
 * @param default The default value of the option.
 * @param title A human-readable title of the option. Example: _MicroG Settings_
 * @param description A human-readable description of the option. Example: _Settings integration for MicroG._
 * @param required Whether the option is required.
 */
@Suppress("MemberVisibilityCanBePrivate")
sealed class PatchOption<T>(
    val key: String,
    default: T?,
    val title: String,
    val description: String,
    val required: Boolean
) {
    var value: T? = default

    /**
     * A [PatchOption] of type [String].
     * @see PatchOption
     */
    class StringOption(
        key: String,
        default: String?,
        title: String,
        description: String,
        required: Boolean = false
    ) : PatchOption<String>(
        key, default, title, description, required
    )

    /**
     * A [PatchOption] of type [Boolean].
     * @see PatchOption
     */
    class BooleanOption(
        key: String,
        default: Boolean?,
        title: String,
        description: String,
        required: Boolean = false
    ) : PatchOption<Boolean>(
        key, default, title, description, required
    )

    /**
     * A [PatchOption] with a list of allowed options.
     * @param options A list of allowed options for the option.
     * @see PatchOption
     */
    sealed class ListOption<E>(
        key: String,
        default: E?,
        val options: Iterable<E>,
        title: String,
        description: String,
        required: Boolean = false
    ) : PatchOption<E>(
        key, default, title, description, required
    ) {
        init {
            if (default !in options) {
                throw IllegalStateException("Default option must be an allowed options")
            }
        }
    }

    /**
     * A [ListOption] of type [String].
     * @see ListOption
     */
    class StringListOption(
        key: String,
        default: String?,
        options: Iterable<String>,
        title: String,
        description: String,
        required: Boolean = false
    ) : ListOption<String>(
        key, default, options, title, description, required
    )

    /**
     * A [ListOption] of type [Int].
     * @see ListOption
     */
    class IntListOption(
        key: String,
        default: Int?,
        options: Iterable<Int>,
        title: String,
        description: String,
        required: Boolean = false
    ) : ListOption<Int>(
        key, default, options, title, description, required
    )
}