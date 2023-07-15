package app.revanced.patcher.usage.bytecode

import app.revanced.patcher.BytecodeContext
import app.revanced.patcher.annotation.Description
import app.revanced.patcher.annotation.Name
import app.revanced.patcher.annotation.Version
import app.revanced.patcher.extensions.InstructionExtensions.addInstructionsWithLabels
import app.revanced.patcher.extensions.InstructionExtensions.replaceInstruction
import app.revanced.patcher.extensions.or
import app.revanced.patcher.patch.BytecodePatch
import app.revanced.patcher.patch.OptionsContainer
import app.revanced.patcher.patch.PatchOption
import app.revanced.patcher.patch.annotations.DependsOn
import app.revanced.patcher.patch.annotations.Patch
import app.revanced.patcher.usage.resource.annotation.ExampleResourceCompatibility
import app.revanced.patcher.usage.resource.patch.ExampleResourcePatch
import app.revanced.patcher.util.proxy.mutableTypes.MutableField.Companion.toMutable
import app.revanced.patcher.util.proxy.mutableTypes.MutableMethod.Companion.toMutable
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

@Patch
@Name("example-bytecode-patch")
@Description("Example demonstration of a bytecode patch.")
@ExampleResourceCompatibility
@Version("0.0.1")
@DependsOn([ExampleResourcePatch::class])
class ExampleBytecodePatch : BytecodePatch(listOf(ExampleFingerprint)) {
    // This function will be executed by the patcher.
    // You can treat it as a constructor
    override suspend fun execute(context: BytecodeContext) {
        // Get the resolved method by its fingerprint from the resolver cache
        val result = ExampleFingerprint.result!!

        // Patch options
        println(key1)
        key2 = false

        // Get the implementation for the resolved method
        val method = result.mutableMethod
        val implementation = method.implementation!!

        // Let's modify it, so it prints "Hello, ReVanced! Editing bytecode."
        // Get the start index of our opcode pattern.
        // This will be the index of the instruction with the opcode CONST_STRING.
        val startIndex = result.scanResult.patternScanResult!!.startIndex

        implementation.replaceStringAt(startIndex, "Hello, ReVanced! Editing bytecode.")

        // Get the class in which the method matching our fingerprint is defined in.
        val mainClass = context.classes.findClassProxied {
            it.type == result.classDef.type
        }!!.mutableClass

        // Add a new method returning a string
        mainClass.methods.add(
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
        method.replaceInstruction(0, "sget-object v0, LTestClass;->dummyField:Ljava/io/PrintStream;")

        // Now let's create a new call to our method and print the return value!
        // You can also use the smali compiler to create instructions.
        // For this sake of example I reuse the TestClass field dummyField inside the virtual register 0.
        //
        // Control flow instructions are not supported as of now.
        method.addInstructionsWithLabels(
            startIndex + 2,
            """
                invoke-static { }, LTestClass;->returnHello()Ljava/lang/String;
                move-result-object v1
                invoke-virtual { v0, v1 }, Ljava/io/PrintStream;->println(Ljava/lang/String;)V
                """
        )
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

    @Suppress("unused")
    companion object : OptionsContainer() {
        private var key1 by option(
            PatchOption.StringOption(
                "key1", "default", "title", "description", true
            )
        )
        private var key2 by option(
            PatchOption.BooleanOption(
                "key2", true, "title", "description" // required defaults to false
            )
        )
        private var key3 by option(
            PatchOption.StringListOption(
                "key3", "TEST", listOf("TEST", "TEST1", "TEST2"), "title", "description"
            )
        )
        private var key4 by option(
            PatchOption.IntListOption(
                "key4", 1, listOf(1, 2, 3), "title", "description"
            )
        )
        private var key5 by option(
            PatchOption.StringOption(
                "key5", null, "title", "description", true
            )
        )
    }
}
