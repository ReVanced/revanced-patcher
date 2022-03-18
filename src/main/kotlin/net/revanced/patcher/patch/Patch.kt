package net.revanced.patcher.patch

class Patch(val name: String, val fn: () -> PatchResult) {
    fun execute(): PatchResult {
        return fn()
    }
}


