package app.revanced.patcher.patch

/**
 * A result of executing a [Patch].
 *
 * @param patch The [Patch] that was executed.
 * @param exception The [PatchException] thrown, if any.
 */
@Suppress("MemberVisibilityCanBePrivate")
class PatchResult internal constructor(val patch: Patch<*>, val exception: PatchException? = null) {
    override fun hashCode() = patch.hashCode()
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as PatchResult

        return patch == other.patch
    }
}