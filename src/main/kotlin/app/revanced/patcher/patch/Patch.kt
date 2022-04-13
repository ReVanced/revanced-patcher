package app.revanced.patcher.patch

import app.revanced.patcher.cache.Cache

abstract class Patch(val metadata: PatchMetadata) {
    abstract fun execute(cache: Cache): PatchResult
}

data class PatchMetadata(
    val shortName: String,
    val fullName: String,
    val description: String,
)