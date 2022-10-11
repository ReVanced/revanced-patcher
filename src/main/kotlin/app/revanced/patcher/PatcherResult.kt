package app.revanced.patcher

import app.revanced.patcher.apk.Apk

/**
 * The result of a patcher.
 * @param files The patched input [Apk]s.
 */
data class PatcherResult(val files: List<Apk>)