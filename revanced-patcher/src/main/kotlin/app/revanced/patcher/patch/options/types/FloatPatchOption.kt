package app.revanced.patcher.patch.options.types

import app.revanced.patcher.patch.Patch
import app.revanced.patcher.patch.options.PatchOption

/**
 * A [PatchOption] representing a [Float].
 *
 * @param key The identifier.
 * @param default The default value.
 * @param title The title.
 * @param description A description.
 * @param required Whether the option is required.
 *
 * @see PatchOption
 */
class FloatPatchOption private constructor(
    key: String,
    default: Float?,
    title: String?,
    description: String?,
    required: Boolean,
    validator: (Float?) -> Boolean
) : PatchOption<Float>(key, default, title, description, required, validator) {
    companion object {
        /**
         * Create a new [FloatPatchOption] and add it to the current [Patch].
         *
         * @param key The identifier.
         * @param default The default value.
         * @param title The title.
         * @param description A description.
         * @param required Whether the option is required.
         * @return The created [FloatPatchOption].
         *
         * @see FloatPatchOption
         * @see PatchOption
         */
        fun <T : Patch<*>> T.floatPatchOption(
            key: String,
            default: Float? = null,
            title: String? = null,
            description: String? = null,
            required: Boolean = false,
            validator: (Float?) -> Boolean = { true }
        ) = FloatPatchOption(key, default, title, description, required, validator).also  { options.register(it) }
    }
}
