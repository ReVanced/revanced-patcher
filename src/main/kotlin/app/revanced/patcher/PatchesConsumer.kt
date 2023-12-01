package app.revanced.patcher

import app.revanced.patcher.patch.Patch

@FunctionalInterface
interface PatchesConsumer {
    @Deprecated("Use acceptPatches(PatchSet) instead.", ReplaceWith("acceptPatches(patches.toSet())"))
    fun acceptPatches(patches: List<Patch<*>>) = acceptPatches(patches.toSet())
    fun acceptPatches(patches: PatchSet)
}
