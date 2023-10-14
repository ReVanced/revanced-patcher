package app.revanced.patcher

/**
 * An exception thrown by ReVanced [Patcher].
 *
 * @param errorMessage The exception message.
 * @param cause The corresponding [Throwable].
 */
sealed class PatcherException(errorMessage: String?, cause: Throwable?) : Exception(errorMessage, cause) {
    constructor(errorMessage: String) : this(errorMessage, null)


    class CircularDependencyException internal constructor(dependant: String) : PatcherException(
        "Patch '$dependant' causes a circular dependency"
    )
}