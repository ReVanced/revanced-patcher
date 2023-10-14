package app.revanced.patcher.patch.options.types

import app.revanced.patcher.patch.Patch
import app.revanced.patcher.patch.options.PatchOption

/**
 * A [PatchOption] representing a [Boolean].
 *
 * @param key The identifier.
 * @param default The default value.
 * @param title The title.
 * @param description A description.
 * @param required Whether the option is required.
 *
 * @see PatchOption
 */
class BooleanPatchOption private constructor(
    key: String,
    default: Boolean?,
    title: String?,
    description: String?,
    required: Boolean,
    validator: (Boolean?) -> Boolean
) : PatchOption<Boolean>(key, default, title, description, required, validator) {
    companion object {
        /**
         * Create a new [BooleanPatchOption] and add it to the current [Patch].
         *
         * @param key The identifier.
         * @param default The default value.
         * @param title The title.
         * @param description A description.
         * @param required Whether the option is required.
         * @return The created [BooleanPatchOption].
         *
         * @see BooleanPatchOption
         * @see PatchOption
         */
        fun <T : Patch<*>> T.booleanPatchOption(
            key: String,
            default: Boolean? = null,
            title: String? = null,
            description: String? = null,
            required: Boolean = false,
            validator: (Boolean?) -> Boolean = { true }
        ) = BooleanPatchOption(key, default, title, description, required, validator).also  { options.register(it) }
    }
}
