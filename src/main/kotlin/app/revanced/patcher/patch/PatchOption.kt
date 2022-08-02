package app.revanced.patcher.patch

@Suppress("CanBeParameter", "MemberVisibilityCanBePrivate")
class NoSuchOptionException(val option: String) : Exception("No such option: $option")

/**
 * A registry for an array of [PatchOption]s.
 * @param options An array of [PatchOption]s.
 */
@Suppress("MemberVisibilityCanBePrivate")
class PatchOptions(vararg val options: PatchOption<*>) : Iterable<PatchOption<*>> {
    private val register = buildMap {
        for (option in options) {
            if (containsKey(option.key)) {
                throw IllegalStateException("Multiple options found with the same key")
            }
            put(option.key, option)
        }
    }

    /**
     * Get a [PatchOption] by its key.
     * @param key The key of the [PatchOption].
     */
    operator fun get(key: String) = register[key] ?: throw NoSuchOptionException(key)

    /**
     * Set the value of a [PatchOption].
     * @param key The key of the [PatchOption].
     * @param value The value you want it to be.
     * Please note that using the wrong value type results in a runtime error.
     */
    inline operator fun <reified T> set(key: String, value: T) {
        @Suppress("UNCHECKED_CAST") val opt = get(key) as? PatchOption<T>
        if (opt == null || opt.value !is T) throw IllegalArgumentException(
            "The type of the option value is not the same as the type value provided"
        )
        opt.value = value
    }

    override fun iterator() = options.iterator()
}

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
     * A [PatchOption] representing a [String].
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
     * A [PatchOption] representing a [Boolean].
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
     * @param options A list of allowed options for the [ListOption].
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