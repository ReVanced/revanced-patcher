package app.revanced.patcher.patch.usage
import app.revanced.patcher.extensions.or
import app.revanced.patcher.fingerprint.method.annotation.FuzzyPatternScanMethod
import app.revanced.patcher.fingerprint.method.impl.MethodFingerprint
import com.android.tools.smali.dexlib2.AccessFlags
import com.android.tools.smali.dexlib2.Opcode

@FuzzyPatternScanMethod(2)
object ExampleFingerprint : MethodFingerprint(
    "V",
    AccessFlags.PUBLIC or AccessFlags.STATIC,
    listOf("[L"),
    listOf(
        Opcode.SGET_OBJECT,
        null,                 // Matching unknown opcodes.
        Opcode.INVOKE_STATIC, // This is intentionally wrong to test fuzzy matching.
        Opcode.RETURN_VOID
    ),
    null
)