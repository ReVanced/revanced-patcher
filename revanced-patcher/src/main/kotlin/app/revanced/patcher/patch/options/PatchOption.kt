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
 * @param validate The function to validate values of the option.
 * @param T The value type of the option.
 */
abstract class PatchOption<T>(
    val key: String,
    default: T?,
    val title: String?,
    val description: String?,
    val required: Boolean,
    val validate: (T?) -> Boolean
) {
    /**
     * The value of the [PatchOption].
     */
    var value: T? = default
        set(value) {
            if (required && value == null) throw PatchOptionException.ValueRequiredException(this)
            if (!validate(value)) throw PatchOptionException.ValueValidationException(value, this)

            field = value
        }

    operator fun getValue(thisRef: Any?, property: KProperty<*>) = value

    operator fun setValue(thisRef: Any?, property: KProperty<*>, value: T?) {
        this.value = value
    }
}