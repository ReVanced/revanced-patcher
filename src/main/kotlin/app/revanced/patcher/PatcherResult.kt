package app.revanced.patcher

/**
 * The result of a patcher.
 * @param files The patched input [Apk]s.
 */
data class PatcherResult(val files: List<Apk>)