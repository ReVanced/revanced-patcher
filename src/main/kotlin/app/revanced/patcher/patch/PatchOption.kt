package app.revanced.patcher.patch

import kotlin.reflect.KProperty

/**
 * A patch option.
 *
 * @param T The value type of the option.
 * @param key The key.
 * @param default The default value.
 * @param values Eligible patch option values mapped to a human-readable name.
 * @param title The title.
 * @param description A description.
 * @param required Whether the option is required.
 * @param valueType The type of the option value (to handle type erasure).
 * @param validator The function to validate the option value.
 *
 * @constructor Create a new [PatchOption].
 */
@Suppress("MemberVisibilityCanBePrivate", "unused")
open class PatchOption<T>(
    val key: String,
    val default: T?,
    val values: Map<String, T?>?,
    val title: String?,
    val description: String?,
    val required: Boolean,
    val valueType: String,
    val validator: PatchOption<T>.(T?) -> Boolean,
) {
    /**
     * The value of the [PatchOption].
     */
    var value: T?
        /**
         * Set the value of the [PatchOption].
         *
         * @param value The value to set.
         *
         * @throws PatchOptionException.ValueRequiredException If the value is required but null.
         * @throws PatchOptionException.ValueValidationException If the value is invalid.
         */
        set(value) {
            assertRequiredButNotNull(value)
            assertValid(value)

            uncheckedValue = value
        }

        /**
         * Get the value of the [PatchOption].
         *
         * @return The value.
         *
         * @throws PatchOptionException.ValueRequiredException If the value is required but null.
         * @throws PatchOptionException.ValueValidationException If the value is invalid.
         */
        get() {
            assertRequiredButNotNull(uncheckedValue)
            assertValid(uncheckedValue)

            return uncheckedValue
        }

    // The unchecked value is used to allow setting the value without validation.
    private var uncheckedValue = default

    /**
     * Reset the [PatchOption] to its default value.
     * Override this method if you need to mutate the value instead of replacing it.
     */
    open fun reset() {
        uncheckedValue = default
    }

    private fun assertRequiredButNotNull(value: T?) {
        if (required && value == null) throw PatchOptionException.ValueRequiredException(this)
    }

    private fun assertValid(value: T?) {
        if (!validator(value)) throw PatchOptionException.ValueValidationException(value, this)
    }

    override fun toString() = value.toString()

    operator fun getValue(
        thisRef: Any?,
        property: KProperty<*>,
    ) = value

    operator fun setValue(
        thisRef: Any?,
        property: KProperty<*>,
        value: T?,
    ) {
        this.value = value
    }
}

/**
 * A collection of [PatchOption]s where patch options can be set and retrieved by key.
 *
 * @param options The patch options.
 *
 * @constructor Create a new [PatchOptions].
 */
class PatchOptions(
    private val options: Map<String, PatchOption<*>>,
) : Map<String, PatchOption<*>> by options {
    constructor(options: Set<PatchOption<*>>) : this(options.associateBy { it.key })

    /**
     * Set a patch option's value.
     *
     * @param key The key.
     * @param value The value.
     *
     * @throws PatchOptionException.PatchOptionNotFoundException If the patch option does not exist.
     */
    operator fun <T : Any> set(key: String, value: T?) {
        val option = this[key]

        try {
            @Suppress("UNCHECKED_CAST")
            (option as PatchOption<T>).value = value
        } catch (e: ClassCastException) {
            throw PatchOptionException.InvalidValueTypeException(
                value?.let { it::class.java.name } ?: "null",
                option.value?.let { it::class.java.name } ?: "null",
            )
        }
    }

    /**
     * Get a patch option.
     *
     * @param key The key.
     *
     * @return The patch option.
     */
    override fun get(key: String) = options[key] ?: throw PatchOptionException.PatchOptionNotFoundException(key)
}

/**
 * Create a new [PatchOption] with a string value and add it to the current [PatchBuilder].
 *
 * @param key The key.
 * @param default The default value.
 * @param values Eligible patch option values mapped to a human-readable name.
 * @param title The title.
 * @param description A description.
 * @param required Whether the option is required.
 * @param validator The function to validate the option value.
 *
 * @return The created [PatchOption].
 *
 * @see PatchOption
 */
fun <P : PatchBuilder<*>> P.stringPatchOption(
    key: String,
    default: String? = null,
    values: Map<String, String?>? = null,
    title: String? = null,
    description: String? = null,
    required: Boolean = false,
    validator: PatchOption<String>.(String?) -> Boolean = { true },
) = addNewPatchOption(
    key,
    default,
    values,
    title,
    description,
    required,
    "String",
    validator,
)

/**
 * Create a new [PatchOption] with an integer value and add it to the current [PatchBuilder].
 *
 * @param key The key.
 * @param default The default value.
 * @param values Eligible patch option values mapped to a human-readable name.
 * @param title The title.
 * @param description A description.
 * @param required Whether the option is required.
 * @param validator The function to validate the option value.
 *
 * @return The created [PatchOption].
 *
 * @see PatchOption
 */
fun <P : PatchBuilder<*>> P.intPatchOption(
    key: String,
    default: Int? = null,
    values: Map<String, Int?>? = null,
    title: String? = null,
    description: String? = null,
    required: Boolean = false,
    validator: PatchOption<Int?>.(Int?) -> Boolean = { true },
) = addNewPatchOption(
    key,
    default,
    values,
    title,
    description,
    required,
    "Int",
    validator,
)

/**
 * Create a new [PatchOption] with a boolean value and add it to the current [PatchBuilder].
 *
 * @param key The key.
 * @param default The default value.
 * @param values Eligible patch option values mapped to a human-readable name.
 * @param title The title.
 * @param description A description.
 * @param required Whether the option is required.
 * @param validator The function to validate the option value.
 *
 * @return The created [PatchOption].
 *
 * @see PatchOption
 */
fun <P : PatchBuilder<*>> P.booleanPatchOption(
    key: String,
    default: Boolean? = null,
    values: Map<String, Boolean?>? = null,
    title: String? = null,
    description: String? = null,
    required: Boolean = false,
    validator: PatchOption<Boolean?>.(Boolean?) -> Boolean = { true },
) = addNewPatchOption(
    key,
    default,
    values,
    title,
    description,
    required,
    "Boolean",
    validator,
)

/**
 * Create a new [PatchOption] with a float value and add it to the current [PatchBuilder].
 *
 * @param key The key.
 * @param default The default value.
 * @param values Eligible patch option values mapped to a human-readable name.
 * @param title The title.
 * @param description A description.
 * @param required Whether the option is required.
 * @param validator The function to validate the option value.
 *
 * @return The created [PatchOption].
 *
 * @see PatchOption
 */
fun <P : PatchBuilder<*>> P.floatPatchOption(
    key: String,
    default: Float? = null,
    values: Map<String, Float?>? = null,
    title: String? = null,
    description: String? = null,
    required: Boolean = false,
    validator: PatchOption<Float?>.(Float?) -> Boolean = { true },
) = addNewPatchOption(
    key,
    default,
    values,
    title,
    description,
    required,
    "Float",
    validator,
)

/**
 * Create a new [PatchOption] with a long value and add it to the current [PatchBuilder].
 *
 * @param key The key.
 * @param default The default value.
 * @param values Eligible patch option values mapped to a human-readable name.
 * @param title The title.
 * @param description A description.
 * @param required Whether the option is required.
 * @param validator The function to validate the option value.
 *
 * @return The created [PatchOption].
 *
 * @see PatchOption
 */
fun <P : PatchBuilder<*>> P.longPatchOption(
    key: String,
    default: Long? = null,
    values: Map<String, Long?>? = null,
    title: String? = null,
    description: String? = null,
    required: Boolean = false,
    validator: PatchOption<Long?>.(Long?) -> Boolean = { true },
) = addNewPatchOption(
    key,
    default,
    values,
    title,
    description,
    required,
    "Long",
    validator,
)

/**
 * Create a new [PatchOption] with a string array value and add it to the current [PatchBuilder].
 *
 * @param key The key.
 * @param default The default value.
 * @param values Eligible patch option values mapped to a human-readable name.
 * @param title The title.
 * @param description A description.
 * @param required Whether the option is required.
 * @param validator The function to validate the option value.
 *
 * @return The created [PatchOption].
 *
 * @see PatchOption
 */
fun <P : PatchBuilder<*>> P.stringArrayPatchOption(
    key: String,
    default: Array<String>? = null,
    values: Map<String, Array<String>?>? = null,
    title: String? = null,
    description: String? = null,
    required: Boolean = false,
    validator: PatchOption<Array<String>?>.(Array<String>?) -> Boolean = { true },
) = addNewPatchOption(
    key,
    default,
    values,
    title,
    description,
    required,
    "StringArray",
    validator,
)

/**
 * Create a new [PatchOption] with an integer array value and add it to the current [PatchBuilder].
 *
 * @param key The key.
 * @param default The default value.
 * @param values Eligible patch option values mapped to a human-readable name.
 * @param title The title.
 * @param description A description.
 * @param required Whether the option is required.
 * @param validator The function to validate the option value.
 *
 * @return The created [PatchOption].
 *
 * @see PatchOption
 */
fun <P : PatchBuilder<*>> P.intArrayPatchOption(
    key: String,
    default: Array<Int>? = null,
    values: Map<String, Array<Int>?>? = null,
    title: String? = null,
    description: String? = null,
    required: Boolean = false,
    validator: PatchOption<Array<Int>?>.(Array<Int>?) -> Boolean = { true },
) = addNewPatchOption(
    key,
    default,
    values,
    title,
    description,
    required,
    "IntArray",
    validator,
)

/**
 * Create a new [PatchOption] with a boolean array value and add it to the current [PatchBuilder].
 *
 * @param key The key.
 * @param default The default value.
 * @param values Eligible patch option values mapped to a human-readable name.
 * @param title The title.
 * @param description A description.
 * @param required Whether the option is required.
 * @param validator The function to validate the option value.
 *
 * @return The created [PatchOption].
 *
 * @see PatchOption
 */
fun <P : PatchBuilder<*>> P.booleanArrayPatchOption(
    key: String,
    default: Array<Boolean>? = null,
    values: Map<String, Array<Boolean>?>? = null,
    title: String? = null,
    description: String? = null,
    required: Boolean = false,
    validator: PatchOption<Array<Boolean>?>.(Array<Boolean>?) -> Boolean = { true },
) = addNewPatchOption(
    key,
    default,
    values,
    title,
    description,
    required,
    "BooleanArray",
    validator,
)

/**
 * Create a new [PatchOption] with a float array value and add it to the current [PatchBuilder].
 *
 * @param key The key.
 * @param default The default value.
 * @param values Eligible patch option values mapped to a human-readable name.
 * @param title The title.
 * @param description A description.
 * @param required Whether the option is required.
 * @param validator The function to validate the option value.
 *
 * @return The created [PatchOption].
 *
 * @see PatchOption
 */
fun <P : PatchBuilder<*>> P.floatArrayPatchOption(
    key: String,
    default: Array<Float>? = null,
    values: Map<String, Array<Float>?>? = null,
    title: String? = null,
    description: String? = null,
    required: Boolean = false,
    validator: PatchOption<Array<Float>?>.(Array<Float>?) -> Boolean = { true },
) = addNewPatchOption(
    key,
    default,
    values,
    title,
    description,
    required,
    "FloatArray",
    validator,
)

/**
 * Create a new [PatchOption] with a long array value and add it to the current [PatchBuilder].
 *
 * @param key The key.
 * @param default The default value.
 * @param values Eligible patch option values mapped to a human-readable name.
 * @param title The title.
 * @param description A description.
 * @param required Whether the option is required.
 * @param validator The function to validate the option value.
 *
 * @return The created [PatchOption].
 *
 * @see PatchOption
 */
fun <P : PatchBuilder<*>> P.longArrayPatchOption(
    key: String,
    default: Array<Long>? = null,
    values: Map<String, Array<Long>?>? = null,
    title: String? = null,
    description: String? = null,
    required: Boolean = false,
    validator: PatchOption<Array<Long>?>.(Array<Long>?) -> Boolean = { true },
) = addNewPatchOption(
    key,
    default,
    values,
    title,
    description,
    required,
    "LongArray",
    validator,
)

/**
 * Create a new [PatchOption] and add it to the current [PatchBuilder].
 *
 * @param key The key.
 * @param default The default value.
 * @param values Eligible patch option values mapped to a human-readable name.
 * @param title The title.
 * @param description A description.
 * @param required Whether the option is required.
 * @param valueType The type of the option value (to handle type erasure).
 * @param validator The function to validate the option value.
 *
 * @return The created [PatchOption].
 *
 * @see PatchOption
 */
fun <P : PatchBuilder<*>, T> P.addNewPatchOption(
    key: String,
    default: T? = null,
    values: Map<String, T?>? = null,
    title: String? = null,
    description: String? = null,
    required: Boolean = false,
    valueType: String,
    validator: PatchOption<T>.(T?) -> Boolean = { true },
) = PatchOption(
    key,
    default,
    values,
    title,
    description,
    required,
    valueType,
    validator,
).also { option(it) }

/**
 * An exception thrown when using [PatchOption]s.
 *
 * @param errorMessage The exception message.
 */
sealed class PatchOptionException(errorMessage: String) : Exception(errorMessage, null) {
    /**
     * An exception thrown when a [PatchOption] is set to an invalid value.
     *
     * @param invalidType The type of the value that was passed.
     * @param expectedType The type of the value that was expected.
     */
    class InvalidValueTypeException(invalidType: String, expectedType: String) :
        PatchOptionException("Type $expectedType was expected but received type $invalidType")

    /**
     * An exception thrown when a value did not satisfy the value conditions specified by the [PatchOption].
     *
     * @param value The value that failed validation.
     */
    class ValueValidationException(value: Any?, option: PatchOption<*>) :
        PatchOptionException("The option value \"$value\" failed validation for ${option.key}")

    /**
     * An exception thrown when a value is required but null was passed.
     *
     * @param option The [PatchOption] that requires a value.
     */
    class ValueRequiredException(option: PatchOption<*>) :
        PatchOptionException("The option ${option.key} requires a value, but null was passed")

    /**
     * An exception thrown when a [PatchOption] is not found.
     *
     * @param key The key of the [PatchOption].
     */
    class PatchOptionNotFoundException(key: String) :
        PatchOptionException("No option with key $key")
}
