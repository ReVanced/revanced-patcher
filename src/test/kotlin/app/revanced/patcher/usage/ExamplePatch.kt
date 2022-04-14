package app.revanced.patcher.usage

import app.revanced.patcher.PatcherData
import app.revanced.patcher.extensions.AccessFlagExtensions.Companion.or
import app.revanced.patcher.extensions.addInstructions
import app.revanced.patcher.patch.Patch
import app.revanced.patcher.patch.PatchMetadata
import app.revanced.patcher.patch.PatchResult
import app.revanced.patcher.patch.PatchResultSuccess
import app.revanced.patcher.proxy.mutableTypes.MutableField.Companion.toMutable
import app.revanced.patcher.proxy.mutableTypes.MutableMethod.Companion.toMutable
import app.revanced.patcher.signature.MethodMetadata
import app.revanced.patcher.signature.MethodSignature
import app.revanced.patcher.signature.MethodSignatureMetadata
import app.revanced.patcher.signature.PatternScanMethod
import app.revanced.patcher.smali.asInstruction
import app.revanced.patcher.smali.asInstructions
import com.google.common.collect.ImmutableList
import org.jf.dexlib2.AccessFlags
import org.jf.dexlib2.Format
import org.jf.dexlib2.Opcode
import org.jf.dexlib2.builder.MutableMethodImplementation
import org.jf.dexlib2.builder.instruction.BuilderInstruction11x
import org.jf.dexlib2.builder.instruction.BuilderInstruction21c
import org.jf.dexlib2.iface.instruction.formats.Instruction21c
import org.jf.dexlib2.immutable.ImmutableField
import org.jf.dexlib2.immutable.ImmutableMethod
import org.jf.dexlib2.immutable.ImmutableMethodImplementation
import org.jf.dexlib2.immutable.reference.ImmutableFieldReference
import org.jf.dexlib2.immutable.reference.ImmutableStringReference
import org.jf.dexlib2.immutable.value.ImmutableFieldEncodedValue
import org.jf.dexlib2.util.Preconditions

class ExamplePatch : Patch(
    metadata = PatchMetadata(
        shortName = "example-patch",
        name = "ReVanced example patch",
        description = "A demonstrative patch to feature the core features of the ReVanced patcher",
        compatiblePackages = arrayOf("com.example.examplePackage"),
        version = "0.0.1"
    ),
    signatures = setOf(
        MethodSignature(
            MethodSignatureMetadata(
                name = "Example signature",
                methodMetadata = MethodMetadata(
                    definingClass = "TestClass",
                    name = "main",
                ),
                patternScanMethod = PatternScanMethod.Fuzzy(2),
                compatiblePackages = arrayOf("com.example.examplePackage"),
                description = "The main method of TestClass",
                version = "1.0.0"
            ),
            returnType = "V",
            accessFlags = AccessFlags.PUBLIC or AccessFlags.STATIC or AccessFlags.STATIC,
            methodParameters = listOf("[L"),
            opcodes = listOf(
                Opcode.CONST_STRING,
                Opcode.INVOKE_STATIC, // This is intentionally wrong to test the Fuzzy resolver.
                Opcode.RETURN_VOID
            )
        )
    )
) {
    // This function will be executed by the patcher.
    // You can treat it as a constructor
    override fun execute(patcherData: PatcherData): PatchResult {

        // Get the resolved method for the signature from the resolver cache
        val result = signatures.first().result!!

        // Get the implementation for the resolved method
        val implementation = result.method.implementation!!

        // Let's modify it, so it prints "Hello, ReVanced! Editing bytecode."
        // Get the start index of our opcode pattern.
        // This will be the index of the instruction with the opcode CONST_STRING.
        val startIndex = result.scanData.startIndex

        implementation.replaceStringAt(startIndex, "Hello, ReVanced! Editing bytecode.")

        // Get the class in which the method matching our signature is defined in.
        val mainClass = patcherData.findClass {
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

    /**
     * Replace the string for an instruction at the given index with a new one.
     * @param index The index of the instruction to replace the string for
     * @param string The replacing string
     */
    private fun MutableMethodImplementation.replaceStringAt(index: Int, string: String) {
        val instruction = this.instructions[index]

        // Utility method of dexlib2
        Preconditions.checkFormat(instruction.opcode, Format.Format21c)

        // Cast this to an instruction of the format 21c
        // The instruction format can be found in the docs at
        // https://source.android.com/devices/tech/dalvik/dalvik-bytecode
        val strInstruction = instruction as Instruction21c

        // In our case we want an instruction with the opcode CONST_STRING
        // The format is 21c, so we create a new BuilderInstruction21c
        // This instruction will hold the string reference constant in the virtual register of the original instruction
        // For that a reference to the string is needed. It can be created with an ImmutableStringReference.
        // At last, use the method replaceInstruction to replace it at the given index startIndex.
        this.replaceInstruction(
            index,
            BuilderInstruction21c(
                Opcode.CONST_STRING,
                strInstruction.registerA,
                ImmutableStringReference(string)
            )
        )
    }
}
