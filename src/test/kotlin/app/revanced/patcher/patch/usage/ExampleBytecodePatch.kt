package app.revanced.patcher.patch.usage

import app.revanced.patcher.data.BytecodeContext
import app.revanced.patcher.extensions.InstructionExtensions.addInstructionsWithLabels
import app.revanced.patcher.extensions.InstructionExtensions.getInstruction
import app.revanced.patcher.extensions.InstructionExtensions.replaceInstruction
import app.revanced.patcher.extensions.or
import app.revanced.patcher.patch.BytecodePatch
import app.revanced.patcher.patch.PatchException
import app.revanced.patcher.patch.annotation.CompatiblePackage
import app.revanced.patcher.patch.annotation.Patch
import app.revanced.patcher.util.proxy.mutableTypes.MutableField.Companion.toMutable
import app.revanced.patcher.util.proxy.mutableTypes.MutableMethod
import app.revanced.patcher.util.proxy.mutableTypes.MutableMethod.Companion.toMutable
import com.android.tools.smali.dexlib2.AccessFlags
import com.android.tools.smali.dexlib2.Format
import com.android.tools.smali.dexlib2.Opcode
import com.android.tools.smali.dexlib2.builder.instruction.BuilderInstruction11x
import com.android.tools.smali.dexlib2.builder.instruction.BuilderInstruction21c
import com.android.tools.smali.dexlib2.iface.instruction.formats.Instruction21c
import com.android.tools.smali.dexlib2.immutable.ImmutableField
import com.android.tools.smali.dexlib2.immutable.ImmutableMethod
import com.android.tools.smali.dexlib2.immutable.ImmutableMethodImplementation
import com.android.tools.smali.dexlib2.immutable.reference.ImmutableFieldReference
import com.android.tools.smali.dexlib2.immutable.reference.ImmutableStringReference
import com.android.tools.smali.dexlib2.immutable.value.ImmutableFieldEncodedValue
import com.android.tools.smali.dexlib2.util.Preconditions
import com.google.common.collect.ImmutableList

@Suppress("unused")
@Patch(
    name = "Example bytecode patch",
    description = "Example demonstration of a bytecode patch.",
    dependencies = [ExampleResourcePatch::class],
    compatiblePackages = [CompatiblePackage("com.example.examplePackage", arrayOf("0.0.1", "0.0.2"))]
)
object ExampleBytecodePatch : BytecodePatch(setOf(ExampleFingerprint)) {
    // Entry point of a patch. Supplied fingerprints are resolved at this point.
    override fun execute(context: BytecodeContext) {
        ExampleFingerprint.result?.let { result ->
            // Let's modify it, so it prints "Hello, ReVanced! Editing bytecode."
            // Get the start index of our opcode pattern.
            // This will be the index of the instruction with the opcode CONST_STRING.
            val startIndex = result.scanResult.patternScanResult!!.startIndex

            result.mutableMethod.apply {
                replaceStringAt(startIndex, "Hello, ReVanced! Editing bytecode.")

                // Store the fields initial value into the first virtual register.
                replaceInstruction(0, "sget-object v0, LTestClass;->dummyField:Ljava/io/PrintStream;")

                // Now let's create a new call to our method and print the return value!
                // You can also use the smali compiler to create instructions.
                // For this sake of example I reuse the TestClass field dummyField inside the virtual register 0.
                //
                // Control flow instructions are not supported as of now.
                addInstructionsWithLabels(
                    startIndex + 2,
                    """
                        invoke-static { }, LTestClass;->returnHello()Ljava/lang/String;
                        move-result-object v1
                        invoke-virtual { v0, v1 }, Ljava/io/PrintStream;->println(Ljava/lang/String;)V
                    """
                )
            }

            // Find the class in which the method matching our fingerprint is defined in.
            context.findClass(result.classDef.type)!!.mutableClass.apply {
                // Add a new method that returns a string.
                methods.add(
                    ImmutableMethod(
                        result.classDef.type,
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

                // Add a field in the main class.
                // We will use this field in our method below to call println on.
                // The field holds the Ljava/io/PrintStream->out; field.
                fields.add(
                    ImmutableField(
                        type,
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
            }
        } ?: throw PatchException("Fingerprint failed to resolve.")
    }

    /**
     * Replace an existing instruction with a new one containing a reference to a new string.
     * @param index The index of the instruction to replace.
     * @param string The replacement string.
     */
    private fun MutableMethod.replaceStringAt(index: Int, string: String) {
        val instruction = getInstruction(index)

        // Utility method of dexlib2.
        Preconditions.checkFormat(instruction.opcode, Format.Format21c)

        // Cast this to an instruction of the format 21c.
        // The instruction format can be found in the docs at
        // https://source.android.com/devices/tech/dalvik/dalvik-bytecode
        val strInstruction = instruction as Instruction21c

        // In our case we want an instruction with the opcode CONST_STRING
        // The format is 21c, so we create a new BuilderInstruction21c
        // This instruction will hold the string reference constant in the virtual register of the original instruction
        // For that a reference to the string is needed. It can be created with an ImmutableStringReference.
        // At last, use the method replaceInstruction to replace it at the given index startIndex.
        replaceInstruction(
            index,
            "const-string ${strInstruction.registerA}, ${ImmutableStringReference(string)}"
        )
    }
}

