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
        // Java:
        // public static void main(String[] args) {
        //     System.out.println("Hello, world!");
        // }
        // Bytecode:
        // public static main(java.lang.String[] arg0) { // Method signature: ([Ljava/lang/String;)V
        //     getstatic java/lang/System.out:java.io.PrintStream
        //     ldc "Hello, world!" (java.lang.String)
        //     invokevirtual java/io/PrintStream.println(Ljava/lang/String;)V
        //     return
        // }
        Signature(
            "mainMethod",
            Type.VOID_TYPE,
            ACC_PUBLIC or ACC_STATIC,
            arrayOf(ExtraTypes.ArrayAny),
            arrayOf(
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
                val main = patcher.cache.methods["mainMethod"]
                val insn = main.method.instructions!!

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