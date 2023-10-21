package app.revanced.patcher.patch.options.types.array

import app.revanced.patcher.patch.Patch
import app.revanced.patcher.patch.options.PatchOption

/**
 * A [PatchOption] representing a [Long] array.
 *
 * @param key The identifier.
 * @param default The default value.
 * @param title The title.
 * @param description A description.
 * @param required Whether the option is required.
 * @param validate The function to validate values of the option.
 *
 * @see PatchOption
 */
class LongArrayPatchOption private constructor(
    key: String,
    default: Array<Long>?,
    title: String?,
    description: String?,
    required: Boolean,
    validate: (Array<Long>?) -> Boolean
) : PatchOption<Array<Long>>(key, default, title, description, required, validate) {
    companion object {
        /**
         * Create a new [LongArrayPatchOption] and add it to the current [Patch].
         *
         * @param key The identifier.
         * @param default The default value.
         * @param title The title.
         * @param description A description.
         * @param required Whether the option is required.
         * @param validate The function to validate values of the option.
         * 
         * @return The created [LongArrayPatchOption].
         *
         * @see LongArrayPatchOption
         * @see PatchOption
         */
        fun <T : Patch<*>> T.longArrayPatchOption(
            key: String,
            default: Array<Long>? = null,
            title: String? = null,
            description: String? = null,
            required: Boolean = false,
            validate: (Array<Long>?) -> Boolean = { true }
        ) = LongArrayPatchOption(key, default, title, description, required, validate).also  { options.register(it) }
    }
}
