package net.revanced.patcher.store

import net.revanced.patcher.patch.Patch

class PatchStore {
    val patches: MutableMap<String, Patch> = mutableMapOf()

    fun addPatches(vararg patches: Patch) {
        for (patch in patches) {
            this.patches[patch.name] = patch
        }
    }
}