package net.revanced.patcher

import net.revanced.patcher.patch.Patch
import net.revanced.patcher.patch.PatchResultSuccess
import net.revanced.patcher.signature.Signature
import net.revanced.patcher.util.ExtraTypes
import net.revanced.patcher.writer.ASMWriter.setAt
import org.objectweb.asm.Opcodes.*
import org.objectweb.asm.Type
import org.objectweb.asm.tree.LdcInsnNode
import java.io.ByteArrayOutputStream
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

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
                // Get the method from the resolver cache
                val main = patcher.cache.methods["mainMethod"]
                // Get the instruction list
                val insn = main.method.instructions!!
                // Let's modify it, so it prints "Hello, ReVanced!"
                // Get the start index of our signature
                // This will be the index of the LDC instruction
                val startIndex = main.sd.startIndex
                insn.setAt(startIndex, LdcInsnNode("Hello, ReVanced!"))
                // Finally, tell the patcher that this patch was a success.
                // You can also return PatchResultError with a message.
                // If an exception is thrown inside this function,
                // a PatchResultError will be returned with the error message.
                PatchResultSuccess()
            }
        )

        val result = patcher.applyPatches()
        for ((s, r) in result) {
            if (r.isFailure) {
                throw Exception("Patch $s failed", r.exceptionOrNull()!!)
            }
        }

        // TODO Doesn't work, needs to be fixed.
//        val out = ByteArrayOutputStream()
//        patcher.saveTo(out)
//        assertTrue(
//            // 8 is a random value, it's just weird if it's any lower than that
//            out.size() > 8,
//            "Output must be at least 8 bytes"
//        )
//
//        out.close()
        testData.close()
    }

    // TODO Doesn't work, needs to be fixed.
//    @Test
//    fun noChanges() {
//        val testData = PatcherTest::class.java.getResourceAsStream("/test1.jar")!!
//        val available = testData.available()
//        val patcher = Patcher(testData, testSigs)
//
//        val out = ByteArrayOutputStream()
//        patcher.saveTo(out)
//        assertEquals(available, out.size())
//
//        out.close()
//        testData.close()
//    }
}