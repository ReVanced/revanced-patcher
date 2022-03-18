package net.revanced.patcher.patch

class Patch(val fn: () -> PatchResult) {
    fun execute(): PatchResult {
        return fn()
    }
}


