package app.revanced.patcher.patch

sealed interface PatchResult {
    object Success : PatchResult
    class Error constructor(errorMessage: String?, cause: Throwable?) : PatchResult,
        Exception(errorMessage, cause) {
        constructor(errorMessage: String) : this(errorMessage, null)
        constructor(cause: Throwable) : this(cause.message, cause)
    }
}