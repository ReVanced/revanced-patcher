package app.revanced.patcher.patch.options

import app.revanced.patcher.patch.Patch
import kotlin.reflect.KProperty

/**
 * A [Patch] option.
 * @param key The identifier.
 * @param default The default value.
 * @param title The title.
 * @param description A description.
 * @param required Whether the option is required.
 * @param validator The function to validate the option value.
 * @param T The value type of the option.
 */
abstract class PatchOption<T>(
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
}