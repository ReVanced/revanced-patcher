package net.revanced.patcher

import net.revanced.patcher.patch.Patch
import net.revanced.patcher.patch.PatchResultError
import net.revanced.patcher.patch.PatchResultSuccess
import net.revanced.patcher.signature.SignatureLoader
import org.junit.jupiter.api.Test

internal class PatcherTest {
    @Test
    fun template() {
        val patcher = Patcher.loadFromFile(
            "some.apk",
            SignatureLoader.LoadFromJson("signatures.json").toMutableList()
        )

        val patches = mutableListOf(
            Patch ("RemoveVideoAds") {
                val videoAdShowMethodInstr = patcher.cache.Methods["SomeMethod"]?.instructions
                PatchResultSuccess()
            },
            Patch ("TweakLayout")  {
                val layoutMethod = patcher.cache.Methods["SomeMethod2"]
                PatchResultError("Failed")
            }
        )

        patcher.executePatches()
    }
}