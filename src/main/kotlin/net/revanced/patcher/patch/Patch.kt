package net.revanced.patcher.patch

abstract class Patch(val patchName: String) {
    abstract fun execute(): PatchResult
}
