package app.revanced.patcher

import app.revanced.patcher.patch.Patch

@FunctionalInterface
interface PatchesConsumer {
    fun acceptPatches(patches: List<Patch<*>>)
}