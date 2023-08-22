package app.revanced.patcher

import app.revanced.patcher.patch.PatchClass

@FunctionalInterface
interface PatchesConsumer {
    fun acceptPatches(patches: List<PatchClass>)
}