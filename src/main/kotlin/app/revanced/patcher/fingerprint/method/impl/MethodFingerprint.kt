package app.revanced.patcher.fingerprint.method.impl

import app.revanced.patcher.data.impl.BytecodeData
import app.revanced.patcher.extensions.softCompareTo
import app.revanced.patcher.fingerprint.Fingerprint
import app.revanced.patcher.fingerprint.method.utils.MethodFingerprintUtils
import app.revanced.patcher.util.proxy.ClassProxy
import org.jf.dexlib2.Opcode
import org.jf.dexlib2.iface.ClassDef
import org.jf.dexlib2.iface.Method

/**
 * Represents the [MethodFingerprint] for a method.
 * @param returnType The return type of the method.
 * @param access The access flags of the method.
 * @param parameters The parameters of the method.
 * @param opcodes The list of opcodes of the method.
 * @param strings A list of strings which a method contains.
 * @param customFingerprint A custom condition for this fingerprint.
 * A `null` opcode is equals to an unknown opcode.
 */
abstract class MethodFingerprint(
    internal val returnType: String? = null,
    internal val access: Int? = null,
    internal val parameters: Iterable<String>? = null,
    internal val opcodes: Iterable<Opcode?>? = null,
    internal val strings: Iterable<String>? = null,
    internal val customFingerprint: ((methodDef: Method) -> Boolean)? = null
) : Fingerprint {
    /**
     * The result of the [MethodFingerprint] the [Method].
     */
    var result: MethodFingerprintResult? = null
}

/**
 * Represents the result of a [MethodFingerprintUtils].
 * @param method The matching method.
 * @param classDef The [ClassDef] that contains the matching [method].
 * @param patternScanResult Opcodes pattern scan result.
 * @param data The [BytecodeData] this [MethodFingerprintResult] is attached to, to create proxies.
 */
data class MethodFingerprintResult(
    val method: Method,
    val classDef: ClassDef,
    val patternScanResult: PatternScanResult?,
    internal val data: BytecodeData
) {
    /**
     * Returns a mutable clone of [classDef]
     *
     * Please note, this method allocates a [ClassProxy].
     * Use [classDef] where possible.
     */
    val mutableClass by lazy { data.proxy(classDef).resolve() }

    /**
     * Returns a mutable clone of [method]
     *
     * Please note, this method allocates a [ClassProxy].
     * Use [method] where possible.
     */
    val mutableMethod by lazy {
        mutableClass.methods.first {
            it.softCompareTo(this.method)
        }
    }
}

/**
 * The result of a pattern scan.
 * @param startIndex The start index of the instructions where to which this pattern matches.
 * @param endIndex The end index of the instructions where to which this pattern matches.
 * @param warnings A list of warnings considering this [PatternScanResult].
 */
data class PatternScanResult(
    val startIndex: Int,
    val endIndex: Int,
    var warnings: List<Warning>? = null
) {
    /**
     * Represents warnings of the pattern scan.
     * @param correctOpcode The opcode the instruction list has.
     * @param wrongOpcode The opcode the pattern list of the signature currently has.
     * @param instructionIndex The index of the opcode relative to the instruction list.
     * @param patternIndex The index of the opcode relative to the pattern list from the signature.
     */
    data class Warning(
        val correctOpcode: Opcode,
        val wrongOpcode: Opcode,
        val instructionIndex: Int,
        val patternIndex: Int,
    )
}