package app.revanced.patcher.patch

/**
 * A result of executing a [Patch].
 *
 * @param patchName The name of the [Patch].
 * @param exception The [PatchException] thrown, if any.
 */
@Suppress("MemberVisibilityCanBePrivate")
class PatchResult internal constructor(val patchName: String, val exception: PatchException? = null)