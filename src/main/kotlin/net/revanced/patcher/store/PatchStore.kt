package net.revanced.patcher.store

import net.revanced.patcher.patch.Patch

object PatchStore {
    val patches: MutableList<Patch> = mutableListOf()

    fun addPatches(vararg patches: Patch) {
        this.patches.addAll(patches)
    }
}