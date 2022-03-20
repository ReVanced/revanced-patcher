package net.revanced.patcher.patch

import net.revanced.patcher.cache.Cache

abstract class Patch(val patchName: String) {
    abstract fun execute(cache: Cache): PatchResult
}
