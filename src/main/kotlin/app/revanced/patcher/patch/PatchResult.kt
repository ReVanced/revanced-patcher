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

class PatchResultError(private val errorMessage: String) : PatchResult {
    fun errorMessage(): String {
        return errorMessage
    }
}

class PatchResultSuccess : PatchResult