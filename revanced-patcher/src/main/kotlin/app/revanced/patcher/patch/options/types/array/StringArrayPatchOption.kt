package app.revanced.patcher.patch.options.types.array

import app.revanced.patcher.patch.Patch
import app.revanced.patcher.patch.options.PatchOption

/**
 * A [PatchOption] representing a [String] array.
 *
 * @param key The identifier.
 * @param default The default value.
 * @param title The title.
 * @param description A description.
 * @param required Whether the option is required.
 *
 * @see PatchOption
 */
class StringArrayPatchOption private constructor(
    key: String,
    default: Array<String>?,
    title: String?,
    description: String?,
    required: Boolean,
    validator: (Array<String>?) -> Boolean
) : PatchOption<Array<String>>(key, default, title, description, required, validator) {
    companion object {
        /**
         * Create a new [StringArrayPatchOption] and add it to the current [Patch].
         *
         * @param key The identifier.
         * @param default The default value.
         * @param title The title.
         * @param description A description.
         * @param required Whether the option is required.
         * @return The created [StringArrayPatchOption].
         *
         * @see StringArrayPatchOption
         * @see PatchOption
         */
        fun <T : Patch<*>> T.stringArrayPatchOption(
            key: String,
            default: Array<String>? = null,
            title: String? = null,
            description: String? = null,
            required: Boolean = false,
            validator: (Array<String>?) -> Boolean = { true }
        ) = StringArrayPatchOption(key, default, title, description, required, validator).also { options.register(it) }
    }
}
