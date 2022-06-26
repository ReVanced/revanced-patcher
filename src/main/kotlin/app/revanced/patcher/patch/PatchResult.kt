package app.revanced.patcher.patch

interface PatchResult {
    fun error(): PatchResultError? {
        if (this is PatchResultError) {
            return this
        }
        return null
    }

    fun success(): PatchResultSuccess? {
        if (this is PatchResultSuccess) {
            return this
        }
        return null
    }

    fun isError(): Boolean {
        return this is PatchResultError
    }

    fun isSuccess(): Boolean {
        return this is PatchResultSuccess
    }
}

class PatchResultError(
    errorMessage: String?, cause: Exception?
) : Exception(errorMessage, cause), PatchResult {
    constructor(errorMessage: String) : this(errorMessage, null)
    constructor(cause: Exception) : this(cause.message, cause)

}

class PatchResultSuccess : PatchResult