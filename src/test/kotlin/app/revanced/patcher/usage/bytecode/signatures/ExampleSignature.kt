package app.revanced.patcher.usage.bytecode.signatures

import app.revanced.patcher.annotation.Name
import app.revanced.patcher.annotation.Version
import app.revanced.patcher.extensions.or
import app.revanced.patcher.signature.implementation.method.MethodSignature
import app.revanced.patcher.signature.implementation.method.annotation.FuzzyPatternScanMethod
import app.revanced.patcher.signature.implementation.method.annotation.MatchingMethod
import app.revanced.patcher.usage.bytecode.annotation.ExampleBytecodeCompatibility
import org.jf.dexlib2.AccessFlags
import org.jf.dexlib2.Opcode

@Name("example-signature")
@MatchingMethod(
    "LexampleClass;",
    "exampleMehod"
)
@FuzzyPatternScanMethod(2)
@ExampleBytecodeCompatibility
@Version("0.0.1")
object ExampleSignature : MethodSignature(
    "V",
    AccessFlags.PUBLIC or AccessFlags.STATIC,
    listOf("[L"),
    listOf(
        Opcode.SGET_OBJECT,
        null,                 // Testing unknown opcodes.
        Opcode.INVOKE_STATIC, // This is intentionally wrong to test the Fuzzy resolver.
        Opcode.RETURN_VOID
    ),
    null
)