package app.revanced.patcher.patch.options

import app.revanced.patcher.patch.Patch
import kotlin.reflect.KProperty

/**
 * A [Patch] option.
 *
 * @param key The key.
 * @param default The default value.
 * @param values Eligible patch option values mapped to a human-readable name.
 * @param title The title.
 * @param description A description.
 * @param required Whether the option is required.
 * @param valueType The type of the option value (to handle type erasure).
 * @param validator The function to validate the option value.
 * @param T The value type of the option.
 */
@Suppress("MemberVisibilityCanBePrivate", "unused")
open class PatchOption<T>(
    val key: String,
    val default: T?,
    val values: Map<String, T?>?,
    val title: String?,
    val description: String?,
    val required: Boolean,
    val valueType: String,
    val validator: PatchOption<T>.(T?) -> Boolean,
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

    override fun toString() = value.toString()

    operator fun getValue(
        thisRef: Any?,
        property: KProperty<*>,
    ) = value

    operator fun setValue(
        thisRef: Any?,
        property: KProperty<*>,
        value: T?,
    ) {
        this.value = value
    }
}
