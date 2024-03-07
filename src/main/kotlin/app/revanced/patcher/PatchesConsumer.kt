package app.revanced.patcher

@FunctionalInterface
interface PatchesConsumer {
    fun acceptPatches(patches: PatchSet)
}
