package app.revanced.arsc

/**
 * An exception thrown when working with [Apk]s.
 *
 * @param message The exception message.
 * @param throwable The corresponding [Throwable].
 */
// TODO: this probably needs a better name but idk what to call it.
sealed class ApkException(message: String, throwable: Throwable? = null) : Exception(message, throwable) {
    /**
     * An exception when decoding resources.
     *
     * @param message The exception message.
     * @param throwable The corresponding [Throwable].
     */
    class Decode(message: String, throwable: Throwable? = null) : ApkException(message, throwable)

    /**
     * An exception when encoding resources.
     *
     * @param message The exception message.
     * @param throwable The corresponding [Throwable].
     */
    class Encode(message: String, throwable: Throwable? = null) : ApkException(message, throwable)

    /**
     * An exception thrown when a reference could not be resolved.
     *
     * @param ref The invalid reference.
     * @param throwable The corresponding [Throwable].
     */
    class InvalidReference(ref: String, throwable: Throwable? = null) :
        ApkException("Failed to resolve: $ref", throwable) {
        constructor(type: String, name: String, throwable: Throwable? = null) : this("@$type/$name", throwable)
    }

    /**
     * An exception thrown when the [Apk] does not have a resource table, but was expected to have one.
     */
    object MissingResourceTable : ApkException("Apk does not have a resource table.")
}