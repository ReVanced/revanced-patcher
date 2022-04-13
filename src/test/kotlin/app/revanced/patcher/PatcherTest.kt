package app.revanced.patcher

import app.revanced.patcher.cache.Cache
import app.revanced.patcher.extensions.AccessFlagExtensions.Companion.or
import app.revanced.patcher.extensions.addInstructions
import app.revanced.patcher.patch.Patch
import app.revanced.patcher.patch.PatchResult
import app.revanced.patcher.patch.PatchResultSuccess
import app.revanced.patcher.proxy.mutableTypes.MutableField.Companion.toMutable
import app.revanced.patcher.proxy.mutableTypes.MutableMethod.Companion.toMutable
import app.revanced.patcher.signature.*
import app.revanced.patcher.smali.asInstruction
import app.revanced.patcher.smali.asInstructions
import com.google.common.collect.ImmutableList
import org.jf.dexlib2.AccessFlags
import org.jf.dexlib2.Opcode
import org.jf.dexlib2.builder.instruction.BuilderInstruction11x
import org.jf.dexlib2.builder.instruction.BuilderInstruction21c
import org.jf.dexlib2.immutable.ImmutableField
import org.jf.dexlib2.immutable.ImmutableMethod
import org.jf.dexlib2.immutable.ImmutableMethodImplementation
import org.jf.dexlib2.immutable.reference.ImmutableFieldReference
import org.jf.dexlib2.immutable.reference.ImmutableStringReference
import org.jf.dexlib2.immutable.value.ImmutableFieldEncodedValue
import org.junit.jupiter.api.Test
import java.io.File
import kotlin.test.assertTrue

internal class PatcherTest {
    companion object {
        val testSignatures = listOf(
            MethodSignature(
                "main-method",
                SignatureMetadata(
                    method = MethodMetadata(
                        definingClass = "TestClass",
                        methodName = "main",
                        comment = "Main method of TestClass. Version 1.0.0"
                    ),
                    patcher = PatcherMetadata(
                        method = ResolverMethod.Fuzzy(2)
                    )
                ),
                "V",
                AccessFlags.PUBLIC or AccessFlags.STATIC or AccessFlags.STATIC,
                listOf("[L"),
                listOf(
                    Opcode.CONST_STRING,
                    Opcode.INVOKE_STATIC, // This is intentionally wrong to test the Fuzzy resolver.
                    Opcode.RETURN_VOID
                )
            )
        )
    }

    @Test
    fun testPatcher() {
        val patcher = Patcher(
            File(PatcherTest::class.java.getResource("/test1.dex")!!.toURI()),
            testSignatures
        )

        patcher.addPatches(listOf(
            object : Patch("TestPatch") {
                override fun execute(cache: Cache): PatchResult {
                    // Get the result from the resolver cache
                    val result = cache.methodMap["main-method"]
                    // Get the implementation for the resolved method
                    val implementation = result.method.implementation!!
                    // Let's modify it, so it prints "Hello, ReVanced! Editing bytecode."
                    // Get the start index of our opcode pattern.
                    // This will be the index of the instruction with the opcode CONST_STRING.
                    val startIndex = result.scanData.startIndex

                    // Replace the instruction at index startIndex with a new instruction.
                    // The instruction format can be found in the docs at
                    // https://source.android.com/devices/tech/dalvik/dalvik-bytecode
                    //
                    // In our case we want an instruction with the opcode CONST_STRING
                    // and the string "Hello, ReVanced! Adding bytecode.".
                    // The format is 21c, so we create a new BuilderInstruction21c
                    // This instruction will hold the string reference constant in the virtual register 1.
                    // For that a reference to the string is needed. It can be created with an ImmutableStringReference.
                    // At last, use the method replaceInstruction to replace it at the given index startIndex.
                    implementation.replaceInstruction(
                        startIndex,
                        BuilderInstruction21c(
                            Opcode.CONST_STRING,
                            1,
                            ImmutableStringReference("Hello, ReVanced! Editing bytecode.")
                        )
                    )

                    // Get the class in which the method matching our signature is defined in.
                    val mainClass = cache.findClass {
                        it.type == result.definingClassProxy.immutableClass.type
                    }!!.resolve()

                    // Add a new method returning a string
                    mainClass.methods.add(
                        ImmutableMethod(
                            result.definingClassProxy.immutableClass.type,
                            "returnHello",
                            null,
                            "Ljava/lang/String;",
                            AccessFlags.PRIVATE or AccessFlags.STATIC,
                            null,
                            null,
                            ImmutableMethodImplementation(
                                1,
                                ImmutableList.of(
                                    BuilderInstruction21c(
                                        Opcode.CONST_STRING,
                                        0,
                                        ImmutableStringReference("Hello, ReVanced! Adding bytecode.")
                                    ),
                                    BuilderInstruction11x(Opcode.RETURN_OBJECT, 0)
                                ),
                                null,
                                null
                            )
                        ).toMutable()
                    )

                    // Add a field in the main class
                    // We will use this field in our method below to call println on
                    // The field holds the Ljava/io/PrintStream->out; field
                    mainClass.fields.add(
                        ImmutableField(
                            mainClass.type,
                            "dummyField",
                            "Ljava/io/PrintStream;",
                            AccessFlags.PRIVATE or AccessFlags.STATIC,
                            ImmutableFieldEncodedValue(
                                ImmutableFieldReference(
                                    "Ljava/lang/System;",
                                    "out",
                                    "Ljava/io/PrintStream;"
                                )
                            ),
                            null,
                            null
                        ).toMutable()
                    )

                    // store the fields initial value into the first virtual register
                    implementation.replaceInstruction(
                        0,
                        "sget-object v0, LTestClass;->dummyField:Ljava/io/PrintStream;".asInstruction()
                    )

                    // Now let's create a new call to our method and print the return value!
                    // You can also use the smali compiler to create instructions.
                    // For this sake of example I reuse the TestClass field dummyField inside the virtual register 0.
                    //
                    // Control flow instructions are not supported as of now.
                    val instructions = """
                        invoke-static { }, LTestClass;->returnHello()Ljava/lang/String;
                        move-result-object v1
                        invoke-virtual { v0, v1 }, Ljava/io/PrintStream;->println(Ljava/lang/String;)V
                    """.trimIndent().asInstructions()
                    implementation.addInstructions(startIndex + 2, instructions)

                    // Finally, tell the patcher that this patch was a success.
                    // You can also return PatchResultError with a message.
                    // If an exception is thrown inside this function,
                    // a PatchResultError will be returned with the error message.
                    return PatchResultSuccess()
                }
            }
        ))

        // Apply all patches loaded in the patcher
        val patchResult = patcher.applyPatches()
        // You can check if an error occurred
        for ((patchName, result) in patchResult) {
            if (result.isFailure) {
                throw Exception("Patch $patchName failed", result.exceptionOrNull()!!)
            }
        }

        val out = patcher.save()
        assertTrue(out.isNotEmpty(), "Expected the output of Patcher#save() to not be empty.")
    }
}
