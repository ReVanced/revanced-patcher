package app.revanced.patcher.patch.options

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
        Exception("The option value \"$value\" failed validation for ${option.key}")

    /**
     * An exception thrown when a value is required but null was passed.
     *
     * @param option The [PatchOption] that requires a value.
     */
    class ValueRequiredException(option: PatchOption<*>) :
        Exception("The option ${option.key} requires a value, but null was passed")

    /**
     * An exception thrown when a [PatchOption] is not found.
     *
     * @param key The key of the [PatchOption].
     */
    class PatchOptionNotFoundException(key: String)
        : Exception("No option with key $key")
}