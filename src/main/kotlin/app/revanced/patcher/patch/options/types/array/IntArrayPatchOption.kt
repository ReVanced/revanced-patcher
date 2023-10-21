package app.revanced.patcher.patch.options.types.array

import app.revanced.patcher.patch.Patch
import app.revanced.patcher.patch.options.PatchOption

/**
 * A [PatchOption] representing an [Integer] array.
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
class IntArrayPatchOption private constructor(
    key: String,
    default: Array<Int>?,
    title: String?,
    description: String?,
    required: Boolean,
    validate: (Array<Int>?) -> Boolean
) : PatchOption<Array<Int>>(key, default, title, description, required, validate) {
    companion object {
        /**
         * Create a new [IntArrayPatchOption] and add it to the current [Patch].
         *
         * @param key The identifier.
         * @param default The default value.
         * @param title The title.
         * @param description A description.
         * @param required Whether the option is required.
         * @param validate The function to validate values of the option.
         * 
         * @return The created [IntArrayPatchOption].
         *
         * @see IntArrayPatchOption
         * @see PatchOption
         */
        fun <T : Patch<*>> T.intArrayPatchOption(
            key: String,
            default: Array<Int>? = null,
            title: String? = null,
            description: String? = null,
            required: Boolean = false,
            validate: (Array<Int>?) -> Boolean = { true }
        ) = IntArrayPatchOption(key, default, title, description, required, validate).also { options.register(it) }
    }
}
