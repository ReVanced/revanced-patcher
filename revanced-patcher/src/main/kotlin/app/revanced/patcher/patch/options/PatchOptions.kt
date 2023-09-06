package app.revanced.patcher.patch.options


/**
 * A map of [PatchOption]s associated by their keys.
 *
 * @param options The [PatchOption]s to initialize with.
 */
class PatchOptions internal constructor(
    private val options: MutableMap<String, PatchOption<*>> = mutableMapOf()
) : MutableMap<String, PatchOption<*>> by options {
    /**
     * Register a [PatchOption]. Acts like [MutableMap.put].
     * @param value The [PatchOption] to register.
     */
    fun register(value: PatchOption<*>) {
        options[value.key] = value
    }

    /**
     * Set an option's value.
     * @param key The identifier.
     * @param value The value.
     * @throws PatchOptionException.PatchOptionNotFoundException If the option does not exist.
     */
    inline operator fun <reified T: Any> set(key: String, value: T?) {
        val option = this[key]

        if (option.value !is T) throw PatchOptionException.InvalidValueTypeException(
            T::class.java.name,
            option.value?.let { it::class.java.name } ?: "null",
        )

        @Suppress("UNCHECKED_CAST")
        (option as PatchOption<T>).value = value
    }

    /**
     * Get an option.
     */
    override operator fun get(key: String) =
        options[key] ?: throw PatchOptionException.PatchOptionNotFoundException(key)
}