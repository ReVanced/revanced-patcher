package app.revanced.patcher.patch.options.types

import app.revanced.patcher.patch.Patch
import app.revanced.patcher.patch.options.PatchOption

/**
 * A [PatchOption] representing a [Long].
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
class LongPatchOption private constructor(
    key: String,
    default: Long?,
    title: String?,
    description: String?,
    required: Boolean,
    validate: (Long?) -> Boolean
) : PatchOption<Long>(key, default, title, description, required, validate) {
    companion object {
        /**
         * Create a new [LongPatchOption] and add it to the current [Patch].
         *
         * @param key The identifier.
         * @param default The default value.
         * @param title The title.
         * @param description A description.
         * @param required Whether the option is required.
         * @return The created [LongPatchOption].
         *
         * @see LongPatchOption
         * @see PatchOption
         */
        fun <T : Patch<*>> T.longPatchOption(
            key: String,
            default: Long? = null,
            title: String? = null,
            description: String? = null,
            required: Boolean = false,
            validate: (Long?) -> Boolean = { true }
        ) = LongPatchOption(key, default, title, description, required, validate).also { options.register(it) }
    }
}
