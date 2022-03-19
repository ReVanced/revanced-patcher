package net.revanced.patcher

import net.revanced.patcher.patch.Patch
import net.revanced.patcher.patch.PatchResultSuccess
import net.revanced.patcher.signature.Signature
import net.revanced.patcher.util.ExtraTypes
import org.junit.jupiter.api.Test
import org.objectweb.asm.Opcodes.*
import org.objectweb.asm.Type

internal class PatcherTest {
    private val testSigs: Array<Signature> = arrayOf(
        Signature(
            "testMethod",
            Type.VOID_TYPE,
            ACC_PUBLIC or ACC_STATIC,
            arrayOf(
                ExtraTypes.ArrayAny,
            ),
            arrayOf(
                GETSTATIC,
                LDC,
                INVOKEVIRTUAL
            )
        )
    )

    @Test
    fun testPatcher() {
        val testData = PatcherTest::class.java.getResourceAsStream("/test1.jar")!!
        val patcher = Patcher(testData, testSigs)

        patcher.addPatches(
            Patch ("TestPatch") {
                patcher.cache.methods["testMethod"]
                PatchResultSuccess()
            }
        )

        val result = patcher.applyPatches()
        for ((s, r) in result) {
            if (r.isFailure) {
                throw Exception("Patch $s failed", r.exceptionOrNull()!!)
            }
        }
    }
}