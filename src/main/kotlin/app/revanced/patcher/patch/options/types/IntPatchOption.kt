package app.revanced.patcher.patch.options.types

import app.revanced.patcher.patch.Patch
import app.revanced.patcher.patch.options.PatchOption

/**
 * A [PatchOption] representing an [Integer].
 *
 * @param key The identifier.
 * @param default The default value.
 * @param title The title.
 * @param description A description.
 * @param required Whether the option is required.
 *
 * @see PatchOption
 */
class IntPatchOption private constructor(
    key: String,
    default: Int?,
    title: String?,
    description: String?,
    required: Boolean,
    validator: (Int?) -> Boolean
) : PatchOption<Int>(key, default, title, description, required, validator) {
    companion object {
        /**
         * Create a new [IntPatchOption] and add it to the current [Patch].
         *
         * @param key The identifier.
         * @param default The default value.
         * @param title The title.
         * @param description A description.
         * @param required Whether the option is required.
         * @return The created [IntPatchOption].
         *
         * @see IntPatchOption
         * @see PatchOption
         */
        fun <T : Patch<*>> T.intPatchOption(
            key: String,
            default: Int? = null,
            title: String? = null,
            description: String? = null,
            required: Boolean = false,
            validator: (Int?) -> Boolean = { true }
        ) = IntPatchOption(key, default, title, description, required, validator).also  { options.register(it) }
    }
}
