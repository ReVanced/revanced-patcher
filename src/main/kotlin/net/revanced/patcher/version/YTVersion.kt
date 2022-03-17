package net.revanced.patcher.version

import net.revanced.patcher.signatures.SignatureSupplier
import net.revanced.patcher.signatures.v17_03_38.Sigs

enum class YTVersion(
    val versionNumber: Triple<Int, Int, Int>,
    val sigs: SignatureSupplier
) {
    V17_03_38(
        Triple(17, 3, 38),
        Sigs()
    );

    companion object {
        private val vm: Map<Triple<Int, Int, Int>, YTVersion> = buildMap {
            values().forEach {
                this[it.versionNumber] = it
            }
        }

        fun versionFor(versionNumber: Triple<Int, Int, Int>): YTVersion? {
            return vm[versionNumber]
        }
    }
}