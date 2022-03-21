package app.revanced.patcher.patch

import app.revanced.patcher.cache.Cache

abstract class Patch(val patchName: String) {
    abstract fun execute(cache: Cache): PatchResult
}
