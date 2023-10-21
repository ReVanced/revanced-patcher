package app.revanced.patcher.patch.options

import app.revanced.patcher.patch.Patch
import kotlin.reflect.KProperty

/**
 * A [Patch] option.
 *
 * @param key The identifier.
 * @param default The default value.
 * @param title The title.
 * @param description A description.
 * @param required Whether the option is required.
 * @param validator The function to validate the option value.
 * @param T The value type of the option.
 */
@Suppress("MemberVisibilityCanBePrivate", "unused")
open class PatchOption<T>(
    val key: String,
    val default: T?,
    val title: String?,
    val description: String?,
    val required: Boolean,
    val validator: (T?) -> Boolean
) {
    /**
     * The value of the [PatchOption].
     */
    var value: T?
        /**
         * Set the value of the [PatchOption].
         *
         * @param value The value to set.
         *
         * @throws PatchOptionException.ValueRequiredException If the value is required but null.
         * @throws PatchOptionException.ValueValidationException If the value is invalid.
         */
        set(value) {
            assertRequiredButNotNull(value)
            assertValid(value)

            uncheckedValue = value
        }
        /**
         * Get the value of the [PatchOption].
         *
         * @return The value.
         *
         * @throws PatchOptionException.ValueRequiredException If the value is required but null.
         * @throws PatchOptionException.ValueValidationException If the value is invalid.
         */
        get() {
            assertRequiredButNotNull(uncheckedValue)
            assertValid(uncheckedValue)

            return uncheckedValue
        }

    // The unchecked value is used to allow setting the value without validation.
    private var uncheckedValue = default

    /**
     * Reset the [PatchOption] to its default value.
     * Override this method if you need to mutate the value instead of replacing it.
     */
    open fun reset() {
        uncheckedValue = default
    }

    private fun assertRequiredButNotNull(value: T?) {
        if (required && value == null) throw PatchOptionException.ValueRequiredException(this)
    }

    private fun assertValid(value: T?) {
        if (!validator(value)) throw PatchOptionException.ValueValidationException(value, this)
    }

    operator fun getValue(thisRef: Any?, property: KProperty<*>) = value

    operator fun setValue(thisRef: Any?, property: KProperty<*>, value: T?) {
        this.value = value
    }

    @Suppress("unused")
    companion object PatchExtensions {
        /**
         * Create a new [PatchOption] with a string value and add it to the current [Patch].
         *
         * @param key The identifier.
         * @param default The default value.
         * @param title The title.
         * @param description A description.
         * @param required Whether the option is required.
         * @param validator The function to validate the option value.
         *
         * @return The created [PatchOption].
         *
         * @see PatchOption
         */
        fun <P : Patch<*>> P.stringPatchOption(
            key: String,
            default: String? = null,
            title: String? = null,
            description: String? = null,
            required: Boolean = false,
            validator: (String?) -> Boolean = { true }
        ) = PatchOption(key, default, title, description, required, validator).also { registerOption(it) }

        /**
         * Create a new [PatchOption] with an integer value and add it to the current [Patch].
         *
         * @param key The identifier.
         * @param default The default value.
         * @param title The title.
         * @param description A description.
         * @param required Whether the option is required.
         * @param validator The function to validate the option value.
         *
         * @return The created [PatchOption].
         *
         * @see PatchOption
         */
        fun <P : Patch<*>> P.intPatchOption(
            key: String,
            default: Int? = null,
            title: String? = null,
            description: String? = null,
            required: Boolean = false,
            validator: (Int?) -> Boolean = { true }
        ) = PatchOption(key, default, title, description, required, validator).also { registerOption(it) }

        /**
         * Create a new [PatchOption] with a boolean value and add it to the current [Patch].
         *
         * @param key The identifier.
         * @param default The default value.
         * @param title The title.
         * @param description A description.
         * @param required Whether the option is required.
         * @param validator The function to validate the option value.
         *
         * @return The created [PatchOption].
         *
         * @see PatchOption
         */
        fun <P : Patch<*>> P.booleanPatchOption(
            key: String,
            default: Boolean? = null,
            title: String? = null,
            description: String? = null,
            required: Boolean = false,
            validator: (Boolean?) -> Boolean = { true }
        ) = PatchOption(key, default, title, description, required, validator).also { registerOption(it) }

        /**
         * Create a new [PatchOption] with a float value and add it to the current [Patch].
         *
         * @param key The identifier.
         * @param default The default value.
         * @param title The title.
         * @param description A description.
         * @param required Whether the option is required.
         * @param validator The function to validate the option value.
         *
         * @return The created [PatchOption].
         *
         * @see PatchOption
         */
        fun <P : Patch<*>> P.floatPatchOption(
            key: String,
            default: Float? = null,
            title: String? = null,
            description: String? = null,
            required: Boolean = false,
            validator: (Float?) -> Boolean = { true }
        ) = PatchOption(key, default, title, description, required, validator).also { registerOption(it) }

        /**
         * Create a new [PatchOption] with a long value and add it to the current [Patch].
         *
         * @param key The identifier.
         * @param default The default value.
         * @param title The title.
         * @param description A description.
         * @param required Whether the option is required.
         * @param validator The function to validate the option value.
         *
         * @return The created [PatchOption].
         *
         * @see PatchOption
         */
        fun <P : Patch<*>> P.longPatchOption(
            key: String,
            default: Long? = null,
            title: String? = null,
            description: String? = null,
            required: Boolean = false,
            validator: (Long?) -> Boolean = { true }
        ) = PatchOption(key, default, title, description, required, validator).also { registerOption(it) }

        /**
         * Create a new [PatchOption] with a string array value and add it to the current [Patch].
         *
         * @param key The identifier.
         * @param default The default value.
         * @param title The title.
         * @param description A description.
         * @param required Whether the option is required.
         * @param validator The function to validate the option value.
         *
         * @return The created [PatchOption].
         *
         * @see PatchOption
         */
        fun <P : Patch<*>> P.stringArrayPatchOption(
            key: String,
            default: Array<String>? = null,
            title: String? = null,
            description: String? = null,
            required: Boolean = false,
            validator: (Array<String>?) -> Boolean = { true }
        ) = PatchOption(key, default, title, description, required, validator).also { registerOption(it) }

        /**
         * Create a new [PatchOption] with an integer array value and add it to the current [Patch].
         *
         * @param key The identifier.
         * @param default The default value.
         * @param title The title.
         * @param description A description.
         * @param required Whether the option is required.
         * @param validator The function to validate the option value.
         *
         * @return The created [PatchOption].
         *
         * @see PatchOption
         */
        fun <P : Patch<*>> P.intArrayPatchOption(
            key: String,
            default: Array<Int>? = null,
            title: String? = null,
            description: String? = null,
            required: Boolean = false,
            validator: (Array<Int>?) -> Boolean = { true }
        ) = PatchOption(key, default, title, description, required, validator).also { registerOption(it) }

        /**
         * Create a new [PatchOption] with a boolean array value and add it to the current [Patch].
         *
         * @param key The identifier.
         * @param default The default value.
         * @param title The title.
         * @param description A description.
         * @param required Whether the option is required.
         * @param validator The function to validate the option value.
         *
         * @return The created [PatchOption].
         *
         * @see PatchOption
         */
        fun <P : Patch<*>> P.booleanArrayPatchOption(
            key: String,
            default: Array<Boolean>? = null,
            title: String? = null,
            description: String? = null,
            required: Boolean = false,
            validator: (Array<Boolean>?) -> Boolean = { true }
        ) = PatchOption(key, default, title, description, required, validator).also { registerOption(it) }

        /**
         * Create a new [PatchOption] with a float array value and add it to the current [Patch].
         *
         * @param key The identifier.
         * @param default The default value.
         * @param title The title.
         * @param description A description.
         * @param required Whether the option is required.
         * @param validator The function to validate the option value.
         *
         * @return The created [PatchOption].
         *
         * @see PatchOption
         */
        fun <P : Patch<*>> P.floatArrayPatchOption(
            key: String,
            default: Array<Float>? = null,
            title: String? = null,
            description: String? = null,
            required: Boolean = false,
            validator: (Array<Float>?) -> Boolean = { true }
        ) = PatchOption(key, default, title, description, required, validator).also { registerOption(it) }

        /**
         * Create a new [PatchOption] with a long array value and add it to the current [Patch].
         *
         * @param key The identifier.
         * @param default The default value.
         * @param title The title.
         * @param description A description.
         * @param required Whether the option is required.
         * @param validator The function to validate the option value.
         *
         * @return The created [PatchOption].
         *
         * @see PatchOption
         */
        fun <P : Patch<*>> P.longArrayPatchOption(
            key: String,
            default: Array<Long>? = null,
            title: String? = null,
            description: String? = null,
            required: Boolean = false,
            validator: (Array<Long>?) -> Boolean = { true }
        ) = PatchOption(key, default, title, description, required, validator).also { registerOption(it) }

        private fun <P : Patch<*>> P.registerOption(option: PatchOption<*>) = option.also { options.register(it) }
    }
}