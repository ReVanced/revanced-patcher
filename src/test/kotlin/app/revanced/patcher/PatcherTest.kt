package app.revanced.patcher

import app.revanced.patcher.cache.Cache
import app.revanced.patcher.extensions.or
import app.revanced.patcher.patch.Patch
import app.revanced.patcher.patch.PatchResult
import app.revanced.patcher.patch.PatchResultSuccess
import app.revanced.patcher.signature.MethodSignature
import org.jf.dexlib2.AccessFlags
import org.jf.dexlib2.Opcode
import org.jf.dexlib2.builder.instruction.BuilderInstruction21c
import org.jf.dexlib2.builder.instruction.BuilderInstruction35c
import org.jf.dexlib2.immutable.reference.ImmutableMethodReference
import org.jf.dexlib2.immutable.reference.ImmutableStringReference
import org.junit.jupiter.api.Test
import java.io.File

internal class PatcherTest {
    companion object {
        val testSignatures: Array<MethodSignature> = arrayOf(
            MethodSignature(
                "main-method",
                "V",
                AccessFlags.PUBLIC or AccessFlags.STATIC,
                setOf("[L"),
                arrayOf(
                    Opcode.CONST_STRING,
                    Opcode.INVOKE_VIRTUAL,
                    Opcode.RETURN_VOID
                )
            )
        )
    }

    @Test
    fun testPatcher() {
        val patcher = Patcher(
            File(PatcherTest::class.java.getResource("/test1.apk")!!.toURI()),
            File("/"),
            testSignatures
        )

        patcher.addPatches(
            object : Patch("TestPatch") {
                override fun execute(cache: Cache): PatchResult {
                    // Get the result from the resolver cache
                    val result = cache.resolvedMethods["main-method"]
                    // Get the implementation for the resolved method
                    val implementation = result.resolveAndGetMethod().implementation!!
                    // Let's modify it, so it prints "Hello, ReVanced! Editing bytecode."
                    // Get the start index of our opcode pattern.
                    // This will be the index of the instruction with the opcode CONST_STRING.
                    val startIndex = result.scanData.startIndex

                    // the instruction format can be found via the docs at https://source.android.com/devices/tech/dalvik/dalvik-bytecode
                    // in our case we want an instruction with the opcode CONST_STRING and the string "Hello, ReVanced! Editing bytecode."
                    // the format is 21c, so we create a new BuilderInstruction21c
                    // with the opcode CONST_STRING and the string "Hello, ReVanced! Editing bytecode."
                    // This instruction will store the constant string reference in the register v1
                    // For that a reference to the string is needed. It can be created by creating a ImmutableStringReference
                    val stringInstruction1 = BuilderInstruction21c(
                        Opcode.CONST_STRING,
                        1,
                        ImmutableStringReference("Hello, ReVanced! Editing bytecode.")
                    )

                    // Replace the instruction at index startIndex with a new instruction
                    // We make sure to use this method to handle references  to it via labels in any case
                    // If we are sure that the instruction is not referenced by any label, we can use the index operator overload
                    // of the instruction list:
                    // implementation.instructions[startIndex] = instruction
                    implementation.replaceInstruction(startIndex, stringInstruction1)

                    // Now lets print our string twice!

                    // Create the necessary instructions (we could also clone the existing ones)
                    val stringInstruction2 = BuilderInstruction21c(
                        Opcode.CONST_STRING,
                        1,
                        ImmutableStringReference("Hello, ReVanced! Adding bytecode.")
                    )
                    val invokeInstruction = BuilderInstruction35c(
                        Opcode.INVOKE_VIRTUAL,
                        2, 0, 1, 0, 0, 0,
                        ImmutableMethodReference(
                            "Ljava.io.PrintStream;",
                            "println",
                            setOf("Ljava/lang/String;"),
                            "V"
                        )
                    )

                    // Insert our instructions after the second instruction by our pattern.
                    implementation.addInstruction(startIndex + 1, stringInstruction2)
                    implementation.addInstruction(startIndex + 3, invokeInstruction)

                    // Finally, tell the patcher that this patch was a success.
                    // You can also return PatchResultError with a message.
                    // If an exception is thrown inside this function,
                    // a PatchResultError will be returned with the error message.
                    return PatchResultSuccess()
                }
            }
        )

        // Apply all patches loaded in the patcher
        val patchResult = patcher.applyPatches()
        // You can check if an error occurred
        for ((patchName, result) in patchResult) {
            if (result.isFailure) {
                throw Exception("Patch $patchName failed", result.exceptionOrNull()!!)
            }
        }

        patcher.save()
    }

    @Test
    fun `test patcher with no changes`() {
        Patcher(
            File(PatcherTest::class.java.getResource("/test1.apk")!!.toURI()),
            File("/no-changes-test"),
            testSignatures
        ).save()
        // FIXME(Sculas): There seems to be a 1-byte difference, not sure what it is.
        // assertEquals(available, out.size())
    }
}