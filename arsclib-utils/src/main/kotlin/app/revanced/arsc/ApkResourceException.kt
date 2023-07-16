package app.revanced.arsc

/**
 * An exception thrown when there is an error with APK resources.
 *
 * @param message The exception message.
 * @param throwable The corresponding [Throwable].
 */
sealed class ApkResourceException(message: String, throwable: Throwable? = null) : Exception(message, throwable) {
    /**
     * An exception when decoding resources.
     *
     * @param message The exception message.
     * @param throwable The corresponding [Throwable].
     */
    class Decode(message: String, throwable: Throwable? = null) : ApkResourceException(message, throwable)

    /**
     * An exception when encoding resources.
     *
     * @param message The exception message.
     * @param throwable The corresponding [Throwable].
     */
    class Encode(message: String, throwable: Throwable? = null) : ApkResourceException(message, throwable)

    /**
     * An exception thrown when a reference could not be resolved.
     *
     * @param reference The invalid reference.
     * @param throwable The corresponding [Throwable].
     */
    class InvalidReference(reference: String, throwable: Throwable? = null) :
        ApkResourceException("Failed to resolve: $reference", throwable) {

        /**
         * An exception thrown when a reference could not be resolved.
         *
         * @param type The type of the reference.
         * @param name The name of the reference.
         * @param throwable The corresponding [Throwable].
         */
        constructor(type: String, name: String, throwable: Throwable? = null) : this("@$type/$name", throwable)
    }

    /**
     * An exception thrown when the Apk file not have a resource table, but was expected to have one.
     */
    object MissingResourceTable : ApkResourceException("Apk does not have a resource table.")
}