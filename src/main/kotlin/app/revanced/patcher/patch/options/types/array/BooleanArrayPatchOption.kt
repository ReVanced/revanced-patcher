package app.revanced.patcher.patch.options.types.array

import app.revanced.patcher.patch.Patch
import app.revanced.patcher.patch.options.PatchOption

/**
 * A [PatchOption] representing a [Boolean] array.
 *
 * @param key The identifier.
 * @param default The default value.
 * @param title The title.
 * @param description A description.
 * @param required Whether the option is required.
 *
 * @see PatchOption
 */
class BooleanArrayPatchOption private constructor(
    key: String,
    default: Array<Boolean>?,
    title: String?,
    description: String?,
    required: Boolean,
    validator: (Array<Boolean>?) -> Boolean
) : PatchOption<Array<Boolean>>(key, default, title, description, required, validator) {
    companion object {
        /**
         * Create a new [BooleanArrayPatchOption] and add it to the current [Patch].
         *
         * @param key The identifier.
         * @param default The default value.
         * @param title The title.
         * @param description A description.
         * @param required Whether the option is required.
         * @return The created [BooleanArrayPatchOption].
         *
         * @see BooleanArrayPatchOption
         * @see PatchOption
         */
        fun <T : Patch<*>> T.booleanArrayPatchOption(
            key: String,
            default: Array<Boolean>? = null,
            title: String? = null,
            description: String? = null,
            required: Boolean = false,
            validator: (Array<Boolean>?) -> Boolean = { true }
        ) = BooleanArrayPatchOption(key, default, title, description, required, validator).also  { options.register(it) }
    }
}
