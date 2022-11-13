package app.revanced.patcher.patch.options

import app.revanced.patcher.extensions.PatchExtensions.patchName
import app.revanced.patcher.patch.Patch
import java.net.URI

class NoSuchOptionException(option: String) : Exception("No such option: $option")

/**
 * A container for patch options.
 */
@Suppress("MemberVisibilityCanBePrivate")
abstract class OptionsContainer {
    private val registry = mutableMapOf<String, PatchOption<*>>()

    fun key() = if (this is Patch<*>) {
        this.javaClass.patchName
    } else {
        this.javaClass.simpleName.lowercase()
    }

    /**
     * Get a [PatchOption] by its key.
     * @param key The key of the [PatchOption].
     */
    @Suppress("UNCHECKED_CAST")
    fun <T> option(key: String) = registry[key] as? PatchOption<T> ?: throw NoSuchOptionException(key)

    fun options(): Collection<PatchOption<*>> = registry.values // downcast to immutable

    protected fun <T> option(
        key: String,
        title: String,
        description: String,
        defaultValue: T
    ): PatchOption<T> {
        val option = PatchOption(key, title, description, defaultValue)
        if (registry.containsKey(option.key)) {
            throw IllegalStateException("Multiple options found with the same key")
        }
        registry[option.key] = option
        return option
    }

    protected fun stringPreference(
        key: String,
        title: String,
        description: String,
        defaultValue: String
    ) = option(key, title, description, defaultValue)

    protected fun booleanPreference(
        key: String,
        title: String,
        description: String,
        defaultValue: Boolean
    ) = option(key, title, description, defaultValue)

    protected fun intPreference(
        key: String,
        title: String,
        description: String,
        defaultValue: Int
    ) = option(key, title, description, defaultValue)

    protected fun floatPreference(
        key: String,
        title: String,
        description: String,
        defaultValue: Float
    ) = option(key, title, description, defaultValue)

    protected inline fun <reified E : Enum<E>> enumPreference(
        key: String,
        title: String,
        description: String,
        defaultValue: E
    ) = option(key, title, description, defaultValue)

    protected fun uriPreference(
        key: String,
        title: String,
        description: String,
        defaultValue: URI
    ) = option(key, title, description, defaultValue)
}