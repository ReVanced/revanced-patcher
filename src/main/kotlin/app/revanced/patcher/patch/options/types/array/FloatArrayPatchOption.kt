package app.revanced.patcher.patch.options.types.array

import app.revanced.patcher.patch.Patch
import app.revanced.patcher.patch.options.PatchOption

/**
 * A [PatchOption] representing a [Float] array.
 *
 * @param key The identifier.
 * @param default The default value.
 * @param title The title.
 * @param description A description.
 * @param required Whether the option is required.
 *
 * @see PatchOption
 */
class FloatArrayPatchOption private constructor(
    key: String,
    default: Array<Float>?,
    title: String?,
    description: String?,
    required: Boolean,
    validator: (Array<Float>?) -> Boolean
) : PatchOption<Array<Float>>(key, default, title, description, required, validator) {
    companion object {
        /**
         * Create a new [FloatArrayPatchOption] and add it to the current [Patch].
         *
         * @param key The identifier.
         * @param default The default value.
         * @param title The title.
         * @param description A description.
         * @param required Whether the option is required.
         * @return The created [FloatArrayPatchOption].
         *
         * @see FloatArrayPatchOption
         * @see PatchOption
         */
        fun <T : Patch<*>> T.floatArrayPatchOption(
            key: String,
            default: Array<Float>? = null,
            title: String? = null,
            description: String? = null,
            required: Boolean = false,
            validator: (Array<Float>?) -> Boolean = { true }
        ) = FloatArrayPatchOption(key, default, title, description, required, validator).also  { options.register(it) }
    }
}
