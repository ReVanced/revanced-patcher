package net.revanced.patcher.patch

class Patch(val patchName: String, val fn: () -> PatchResult) {
    fun execute(): PatchResult {
        return fn()
    }
}


