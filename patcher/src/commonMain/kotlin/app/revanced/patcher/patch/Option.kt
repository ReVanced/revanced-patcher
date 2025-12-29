@file:Suppress("unused")

package app.revanced.patcher.patch

import kotlin.reflect.KProperty
import kotlin.reflect.KType
import kotlin.reflect.jvm.jvmName
import kotlin.reflect.typeOf

/**
 * An option.
 *
 * @param T The value type of the option.
 * @param name The name.
 * @param default The default value.
 * @param values Eligible option values mapped to a human-readable name.
 * @param description A description.
 * @param required Whether the option is required.
 * @param type The type of the option value (to handle type erasure).
 * @param validator The function to validate the option value.
 *
 * @constructor Create a new [Option].
 */
@Suppress("MemberVisibilityCanBePrivate", "unused")
class Option<T>
@PublishedApi
internal constructor(
    val name: String,
    val default: T? = null,
    val values: Map<String, T?>? = null,
    val description: String? = null,
    val required: Boolean = false,
    val type: KType,
    val validator: Option<T>.(T?) -> Boolean = { true },
) {
    /**
     * The value of the [Option].
     */
    var value: T?
        /**
         * Set the value of the [Option].
         *
         * @param value The value to set.
         *
         * @throws OptionException.ValueRequiredException If the value is required but null.
         * @throws OptionException.ValueValidationException If the value is invalid.
         */
        set(value) {
            assertRequiredButNotNull(value)
            assertValid(value)

            uncheckedValue = value
        }
        /**
         * Get the value of the [Option].
         *
         * @return The value.
         *
         * @throws OptionException.ValueRequiredException If the value is required but null.
         * @throws OptionException.ValueValidationException If the value is invalid.
         */
        get() {
            assertRequiredButNotNull(uncheckedValue)
            assertValid(uncheckedValue)

            return uncheckedValue
        }

    // The unchecked value is used to allow setting the value without validation.
    private var uncheckedValue = default

    /**
     * Reset the [Option] to its default value.
     * Override this method if you need to mutate the value instead of replacing it.
     */
    fun reset() {
        uncheckedValue = default
    }

    private fun assertRequiredButNotNull(value: T?) {
        if (required && value == null) throw OptionException.ValueRequiredException(this)
    }

    private fun assertValid(value: T?) {
        if (!validator(value)) throw OptionException.ValueValidationException(value, this)
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
 * A collection of [Option]s where options can be set and retrieved by their key.
 *
 * @param options The options.
 *
 * @constructor Create a new [Options].
 */
class Options internal constructor(
    private val options: Map<String, Option<*>>,
) : Map<String, Option<*>> by options {
    internal constructor(options: Set<Option<*>>) : this(options.associateBy { it.name })

    /**
     * Set an option's value.
     *
     * @param name The name.
     * @param value The value.
     *
     * @throws OptionException.OptionNotFoundException If the option does not exist.
     */
    operator fun <T : Any> set(name: String, value: T?) {
        val option = this[name]

        try {
            @Suppress("UNCHECKED_CAST")
            (option as Option<T>).value = value
        } catch (e: ClassCastException) {
            throw OptionException.InvalidValueTypeException(
                value?.let { it::class.jvmName } ?: "null",
                option.value?.let { it::class.jvmName } ?: "null",
            )
        }
    }

    /**
     * Get an option.
     *
     * @param key The name.
     *
     * @return The option.
     */
    override fun get(key: String) = options[key] ?: throw OptionException.OptionNotFoundException(key)
}

/**
 * Create a new [Option] with a string value.
 *
 * @param name The name.
 * @param default The default value.
 * @param values Eligible option values mapped to a human-readable name.
 * @param description A description.
 * @param required Whether the option is required.
 * @param validator The function to validate the option value.
 *
 * @return The created [Option].
 *
 * @see Option
 */
fun stringOption(
    name: String,
    default: String? = null,
    values: Map<String, String?>? = null,
    description: String? = null,
    required: Boolean = false,
    validator: Option<String>.(String?) -> Boolean = { true },
) = option(
    name,
    default,
    values,
    description,
    required,
    validator,
)

/**
 * Create a new [Option] with a string value and add it to the current [PatchBuilder].
 *
 * @param name The name.
 * @param default The default value.
 * @param values Eligible option values mapped to a human-readable name.
 * @param description A description.
 * @param required Whether the option is required.
 * @param validator The function to validate the option value.
 *
 * @return The created [Option].
 *
 * @see Option
 */
fun PatchBuilder<*>.stringOption(
    name: String,
    default: String? = null,
    values: Map<String, String?>? = null,
    description: String? = null,
    required: Boolean = false,
    validator: Option<String>.(String?) -> Boolean = { true },
) = option(
    name,
    default,
    values,
    description,
    required,
    validator,
)

/**
 * Create a new [Option] with an integer value.
 *
 * @param name The name.
 * @param default The default value.
 * @param values Eligible option values mapped to a human-readable name.
 * @param description A description.
 * @param required Whether the option is required.
 * @param validator The function to validate the option value.
 *
 * @return The created [Option].
 *
 * @see Option
 */
fun intOption(
    name: String,
    default: Int? = null,
    values: Map<String, Int?>? = null,
    description: String? = null,
    required: Boolean = false,
    validator: Option<Int>.(Int?) -> Boolean = { true },
) = option(
    name,
    default,
    values,
    description,
    required,
    validator,
)

/**
 * Create a new [Option] with an integer value and add it to the current [PatchBuilder].
 *
 * @param name The name.
 * @param default The default value.
 * @param values Eligible option values mapped to a human-readable name.
 * @param description A description.
 * @param required Whether the option is required.
 * @param validator The function to validate the option value.
 *
 * @return The created [Option].
 *
 * @see Option
 */
fun PatchBuilder<*>.intOption(
    name: String,
    default: Int? = null,
    values: Map<String, Int?>? = null,
    description: String? = null,
    required: Boolean = false,
    validator: Option<Int>.(Int?) -> Boolean = { true },
) = option(
    name,
    default,
    values,
    description,
    required,
    validator,
)

/**
 * Create a new [Option] with a boolean value.
 *
 * @param name The name.
 * @param default The default value.
 * @param values Eligible option values mapped to a human-readable name.
 * @param description A description.
 * @param required Whether the option is required.
 * @param validator The function to validate the option value.
 *
 * @return The created [Option].
 *
 * @see Option
 */
fun booleanOption(
    name: String,
    default: Boolean? = null,
    values: Map<String, Boolean?>? = null,
    description: String? = null,
    required: Boolean = false,
    validator: Option<Boolean>.(Boolean?) -> Boolean = { true },
) = option(
    name,
    default,
    values,
    description,
    required,
    validator,
)

/**
 * Create a new [Option] with a boolean value and add it to the current [PatchBuilder].
 *
 * @param name The name.
 * @param default The default value.
 * @param values Eligible option values mapped to a human-readable name.
 * @param description A description.
 * @param required Whether the option is required.
 * @param validator The function to validate the option value.
 *
 * @return The created [Option].
 *
 * @see Option
 */
fun PatchBuilder<*>.booleanOption(
    name: String,
    default: Boolean? = null,
    values: Map<String, Boolean?>? = null,
    description: String? = null,
    required: Boolean = false,
    validator: Option<Boolean>.(Boolean?) -> Boolean = { true },
) = option(
    name,
    default,
    values,
    description,
    required,
    validator,
)

/**
 * Create a new [Option] with a float value.
 *
 * @param name The name.
 * @param default The default value.
 * @param values Eligible option values mapped to a human-readable name.
 * @param description A description.
 * @param required Whether the option is required.
 * @param validator The function to validate the option value.
 *
 * @return The created [Option].
 *
 * @see Option
 */
fun floatOption(
    name: String,
    default: Float? = null,
    values: Map<String, Float?>? = null,
    description: String? = null,
    required: Boolean = false,
    validator: Option<Float>.(Float?) -> Boolean = { true },
) = option(
    name,
    default,
    values,
    description,
    required,
    validator,
)

/**
 * Create a new [Option] with a float value and add it to the current [PatchBuilder].
 *
 * @param name The name.
 * @param default The default value.
 * @param values Eligible option values mapped to a human-readable name.
 * @param description A description.
 * @param required Whether the option is required.
 * @param validator The function to validate the option value.
 *
 * @return The created [Option].
 *
 * @see Option
 */
fun PatchBuilder<*>.floatOption(
    name: String,
    default: Float? = null,
    values: Map<String, Float?>? = null,
    description: String? = null,
    required: Boolean = false,
    validator: Option<Float>.(Float?) -> Boolean = { true },
) = option(
    name,
    default,
    values,
    description,
    required,
    validator,
)

/**
 * Create a new [Option] with a long value.
 *
 * @param name The name.
 * @param default The default value.
 * @param values Eligible option values mapped to a human-readable name.
 * @param description A description.
 * @param required Whether the option is required.
 * @param validator The function to validate the option value.
 *
 * @return The created [Option].
 *
 * @see Option
 */
fun longOption(
    name: String,
    default: Long? = null,
    values: Map<String, Long?>? = null,
    description: String? = null,
    required: Boolean = false,
    validator: Option<Long>.(Long?) -> Boolean = { true },
) = option(
    name,
    default,
    values,
    description,
    required,
    validator,
)

/**
 * Create a new [Option] with a long value and add it to the current [PatchBuilder].
 *
 * @param name The name.
 * @param default The default value.
 * @param values Eligible option values mapped to a human-readable name.
 * @param description A description.
 * @param required Whether the option is required.
 * @param validator The function to validate the option value.
 *
 * @return The created [Option].
 *
 * @see Option
 */
fun PatchBuilder<*>.longOption(
    name: String,
    default: Long? = null,
    values: Map<String, Long?>? = null,
    description: String? = null,
    required: Boolean = false,
    validator: Option<Long>.(Long?) -> Boolean = { true },
) = option(
    name,
    default,
    values,
    description,
    required,
    validator,
)

/**
 * Create a new [Option] with a string list value.
 *
 * @param name The name.
 * @param default The default value.
 * @param values Eligible option values mapped to a human-readable name.
 * @param description A description.
 * @param required Whether the option is required.
 * @param validator The function to validate the option value.
 *
 * @return The created [Option].
 *
 * @see Option
 */
fun stringsOption(
    name: String,
    default: List<String>? = null,
    values: Map<String, List<String>?>? = null,
    description: String? = null,
    required: Boolean = false,
    validator: Option<List<String>>.(List<String>?) -> Boolean = { true },
) = option(
    name,
    default,
    values,
    description,
    required,
    validator,
)

/**
 * Create a new [Option] with a string list value and add it to the current [PatchBuilder].
 *
 * @param name The name.
 * @param default The default value.
 * @param values Eligible option values mapped to a human-readable name.
 * @param description A description.
 * @param required Whether the option is required.
 * @param validator The function to validate the option value.
 *
 * @return The created [Option].
 *
 * @see Option
 */
fun PatchBuilder<*>.stringsOption(
    name: String,
    default: List<String>? = null,
    values: Map<String, List<String>?>? = null,
    description: String? = null,
    required: Boolean = false,
    validator: Option<List<String>>.(List<String>?) -> Boolean = { true },
) = option(
    name,
    default,
    values,
    description,
    required,
    validator,
)

/**
 * Create a new [Option] with an integer list value.
 *
 * @param name The name.
 * @param default The default value.
 * @param values Eligible option values mapped to a human-readable name.
 * @param description A description.
 * @param required Whether the option is required.
 * @param validator The function to validate the option value.
 *
 * @return The created [Option].
 *
 * @see Option
 */
fun intsOption(
    name: String,
    default: List<Int>? = null,
    values: Map<String, List<Int>?>? = null,
    description: String? = null,
    required: Boolean = false,
    validator: Option<List<Int>>.(List<Int>?) -> Boolean = { true },
) = option(
    name,
    default,
    values,
    description,
    required,
    validator,
)

/**
 * Create a new [Option] with an integer list value and add it to the current [PatchBuilder].
 *
 * @param name The name.
 * @param default The default value.
 * @param values Eligible option values mapped to a human-readable name.
 * @param description A description.
 * @param required Whether the option is required.
 * @param validator The function to validate the option value.
 *
 * @return The created [Option].
 *
 * @see Option
 */
fun PatchBuilder<*>.intsOption(
    name: String,
    default: List<Int>? = null,
    values: Map<String, List<Int>?>? = null,
    description: String? = null,
    required: Boolean = false,
    validator: Option<List<Int>>.(List<Int>?) -> Boolean = { true },
) = option(
    name,
    default,
    values,
    description,
    required,
    validator,
)

/**
 * Create a new [Option] with a boolean list value.
 *
 * @param name The name.
 * @param default The default value.
 * @param values Eligible option values mapped to a human-readable name.
 * @param description A description.
 * @param required Whether the option is required.
 * @param validator The function to validate the option value.
 *
 * @return The created [Option].
 *
 * @see Option
 */
fun booleansOption(
    name: String,
    default: List<Boolean>? = null,
    values: Map<String, List<Boolean>?>? = null,
    description: String? = null,
    required: Boolean = false,
    validator: Option<List<Boolean>>.(List<Boolean>?) -> Boolean = { true },
) = option(
    name,
    default,
    values,
    description,
    required,
    validator,
)

/**
 * Create a new [Option] with a boolean list value and add it to the current [PatchBuilder].
 *
 * @param name The name.
 * @param default The default value.
 * @param values Eligible option values mapped to a human-readable name.
 * @param description A description.
 * @param required Whether the option is required.
 * @param validator The function to validate the option value.
 *
 * @return The created [Option].
 *
 * @see Option
 */
fun PatchBuilder<*>.booleansOption(
    name: String,
    default: List<Boolean>? = null,
    values: Map<String, List<Boolean>?>? = null,
    description: String? = null,
    required: Boolean = false,
    validator: Option<List<Boolean>>.(List<Boolean>?) -> Boolean = { true },
) = option(
    name,
    default,
    values,
    description,
    required,
    validator,
)

/**
 * Create a new [Option] with a float list value and add it to the current [PatchBuilder].
 *
 * @param name The name.
 * @param default The default value.
 * @param values Eligible option values mapped to a human-readable name.
 * @param description A description.
 * @param required Whether the option is required.
 * @param validator The function to validate the option value.
 *
 * @return The created [Option].
 *
 * @see Option
 */
fun PatchBuilder<*>.floatsOption(
    name: String,
    default: List<Float>? = null,
    values: Map<String, List<Float>?>? = null,
    description: String? = null,
    required: Boolean = false,
    validator: Option<List<Float>>.(List<Float>?) -> Boolean = { true },
) = option(
    name,
    default,
    values,
    description,
    required,
    validator,
)

/**
 * Create a new [Option] with a long list value.
 *
 * @param name The name.
 * @param default The default value.
 * @param values Eligible option values mapped to a human-readable name.
 * @param description A description.
 * @param required Whether the option is required.
 * @param validator The function to validate the option value.
 *
 * @return The created [Option].
 *
 * @see Option
 */
fun longsOption(
    name: String,
    default: List<Long>? = null,
    values: Map<String, List<Long>?>? = null,
    description: String? = null,
    required: Boolean = false,
    validator: Option<List<Long>>.(List<Long>?) -> Boolean = { true },
) = option(
    name,
    default,
    values,
    description,
    required,
    validator,
)

/**
 * Create a new [Option] with a long list value and add it to the current [PatchBuilder].
 *
 * @param name The name.
 * @param default The default value.
 * @param values Eligible option values mapped to a human-readable name.
 * @param description A description.
 * @param required Whether the option is required.
 * @param validator The function to validate the option value.
 *
 * @return The created [Option].
 *
 * @see Option
 */
fun PatchBuilder<*>.longsOption(
    name: String,
    default: List<Long>? = null,
    values: Map<String, List<Long>?>? = null,
    description: String? = null,
    required: Boolean = false,
    validator: Option<List<Long>>.(List<Long>?) -> Boolean = { true },
) = option(
    name,
    default,
    values,
    description,
    required,
    validator,
)

/**
 * Create a new [Option].
 *
 * @param name The name.
 * @param default The default value.
 * @param values Eligible option values mapped to a human-readable name.
 * @param description A description.
 * @param required Whether the option is required.
 * @param validator The function to validate the option value.
 *
 * @return The created [Option].
 *
 * @see Option
 */
inline fun <reified T> option(
    name: String,
    default: T? = null,
    values: Map<String, T?>? = null,
    description: String? = null,
    required: Boolean = false,
    noinline validator: Option<T>.(T?) -> Boolean = { true },
) = Option(
    name,
    default,
    values,
    description,
    required,
    typeOf<T>(),
    validator,
)

/**
 * Create a new [Option] and add it to the current [PatchBuilder].
 *
 * @param name The name.
 * @param default The default value.
 * @param values Eligible option values mapped to a human-readable name.
 * @param description A description.
 * @param required Whether the option is required.
 * @param validator The function to validate the option value.
 *
 * @return The created [Option].
 *
 * @see Option
 */
inline fun <reified T> PatchBuilder<*>.option(
    name: String,
    default: T? = null,
    values: Map<String, T?>? = null,
    description: String? = null,
    required: Boolean = false,
    noinline validator: Option<T>.(T?) -> Boolean = { true },
) = app.revanced.patcher.patch.option(
    name,
    default,
    values,
    description,
    required,
    validator,
).also { it() }

/**
 * An exception thrown when using [Option]s.
 *
 * @param errorMessage The exception message.
 */
sealed class OptionException(errorMessage: String) : Exception(errorMessage, null) {
    /**
     * An exception thrown when a [Option] is set to an invalid value.
     *
     * @param invalidType The type of the value that was passed.
     * @param expectedType The type of the value that was expected.
     */
    class InvalidValueTypeException(invalidType: String, expectedType: String) :
        OptionException("Type $expectedType was expected but received type $invalidType")

    /**
     * An exception thrown when a value did not satisfy the value conditions specified by the [Option].
     *
     * @param value The value that failed validation.
     */
    class ValueValidationException(value: Any?, option: Option<*>) :
        OptionException("The option value \"$value\" failed validation for ${option.name}")

    /**
     * An exception thrown when a value is required but null was passed.
     *
     * @param option The [Option] that requires a value.
     */
    class ValueRequiredException(option: Option<*>) :
        OptionException("The option ${option.name} requires a value, but the value was null")

    /**
     * An exception thrown when a [Option] is not found.
     *
     * @param name The name of the [Option].
     */
    class OptionNotFoundException(name: String) : OptionException("No option with name $name")
}
