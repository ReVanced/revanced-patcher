package app.revanced.patcher.usage.bytecode

import app.revanced.patcher.annotation.Name
import app.revanced.patcher.annotation.Version
import app.revanced.patcher.extensions.or
import app.revanced.patcher.fingerprint.method.annotation.FuzzyPatternScanMethod
import app.revanced.patcher.fingerprint.method.annotation.MatchingMethod
import app.revanced.patcher.fingerprint.method.impl.MethodFingerprint
import app.revanced.patcher.usage.bytecode.ExampleBytecodeCompatibility
import org.jf.dexlib2.AccessFlags
import org.jf.dexlib2.Opcode

@Name("example-fingerprint")
@MatchingMethod(
    "LexampleClass;",
    "exampleMehod"
)
@FuzzyPatternScanMethod(2)
@ExampleBytecodeCompatibility
@Version("0.0.1")
object ExampleFingerprint : MethodFingerprint(
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