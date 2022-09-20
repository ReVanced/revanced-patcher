package app.revanced.patcher.fingerprint.method.utils

import app.revanced.patcher.data.impl.BytecodeData
import app.revanced.patcher.extensions.MethodFingerprintExtensions.fuzzyPatternScanMethod
import app.revanced.patcher.extensions.MethodFingerprintExtensions.fuzzyScanThreshold
import app.revanced.patcher.extensions.parametersEqual
import app.revanced.patcher.fingerprint.method.annotation.FuzzyPatternScanMethod
import app.revanced.patcher.fingerprint.method.impl.MethodFingerprint
import app.revanced.patcher.fingerprint.method.impl.MethodFingerprintResult
import org.jf.dexlib2.Opcode
import org.jf.dexlib2.iface.ClassDef
import org.jf.dexlib2.iface.Method
import org.jf.dexlib2.iface.instruction.Instruction
import org.jf.dexlib2.iface.instruction.ReferenceInstruction
import org.jf.dexlib2.iface.reference.StringReference
/**
 * Utility class for [MethodFingerprint]
 */
object MethodFingerprintUtils {
    /**
     * Resolve a list of [MethodFingerprint] against a list of [ClassDef].
     * @param context The classes on which to resolve the [MethodFingerprint].
     * @param forData The [BytecodeData] to host proxies.
     * @return True if the resolution was successful, false otherwise.
     */
    fun Iterable<MethodFingerprint>.resolve(forData: BytecodeData, context: Iterable<ClassDef>) {
        for (fingerprint in this) // For each fingerprint
            classes@ for (classDef in context) // search through all classes for the fingerprint
                if (fingerprint.resolve(forData, classDef))
                    break@classes // if the resolution succeeded, continue with the next fingerprint
    }

    /**
     * Resolve a [MethodFingerprint] against a [ClassDef].
     * @param context The class on which to resolve the [MethodFingerprint].
     * @param forData The [BytecodeData] to host proxies.
     * @return True if the resolution was successful, false otherwise.
     */
    fun MethodFingerprint.resolve(forData: BytecodeData, context: ClassDef): Boolean {
        for (method in context.methods)
            if (this.resolve(forData, method, context))
                return true
        return false
    }

    /**
     * Resolve a [MethodFingerprint] against a [Method].
     * @param context The context on which to resolve the [MethodFingerprint].
     * @param classDef The class of the matching [Method].
     * @param forData The [BytecodeData] to host proxies.
     * @return True if the resolution was successful or if the fingerprint is already resolved, false otherwise.
     */
    fun MethodFingerprint.resolve(forData: BytecodeData, context: Method, classDef: ClassDef): Boolean {
        val methodFingerprint = this

        if (methodFingerprint.result != null) return true

        if (methodFingerprint.returnType != null && !context.returnType.startsWith(methodFingerprint.returnType))
            return false

        if (methodFingerprint.access != null && methodFingerprint.access != context.accessFlags)
            return false


        if (methodFingerprint.parameters != null && !parametersEqual(
                methodFingerprint.parameters, // TODO: parseParameters()
                context.parameterTypes
            )
        ) return false

        @Suppress("UNNECESSARY_NOT_NULL_ASSERTION")
        if (methodFingerprint.customFingerprint != null && !methodFingerprint.customFingerprint!!(context))
            return false

        val stringsScanResult: StringsScanResult? =
            if (methodFingerprint.strings != null) {
                StringsScanResult(buildList {
                    val implementation = context.implementation ?: return false

                    val stringsList = methodFingerprint.strings.toMutableList()

                    implementation.instructions.forEach { instruction ->
                        if (instruction.opcode.ordinal != Opcode.CONST_STRING.ordinal) return@forEach

                        val string = ((instruction as ReferenceInstruction).reference as StringReference).string
                        val index = stringsList.indexOfFirst { it == string }
                        if (index == -1) return@forEach

                        add(
                            StringMatch(
                                string,
                                index
                            )
                        )
                        stringsList.removeAt(index)
                    }

                    if (stringsList.isNotEmpty()) return false
                })
            } else null

        val patternScanResult = if (methodFingerprint.opcodes != null) {
            context.implementation?.instructions ?: return false

            context.patternScan(methodFingerprint) ?: return false
        } else null

        methodFingerprint.result = MethodFingerprintResult(
            context,
            classDef,
            MethodFingerprintResult.MethodFingerprintScanResult(
                patternScanResult,
                stringsScanResult
            ),
            forData
        )

        return true
    }

    private fun Method.patternScan(
        fingerprint: MethodFingerprint
    ): MethodFingerprintResult.MethodFingerprintScanResult.PatternScanResult? {
        val instructions = this.implementation!!.instructions
        val fingerprintFuzzyPatternScanThreshold = fingerprint.fuzzyScanThreshold

        val pattern = fingerprint.opcodes!!
        val instructionLength = instructions.count()
        val patternLength = pattern.count()

        for (index in 0 until instructionLength) {
            var patternIndex = 0
            var threshold = fingerprintFuzzyPatternScanThreshold

            while (index + patternIndex < instructionLength) {
                val originalOpcode = instructions.elementAt(index + patternIndex).opcode
                val patternOpcode = pattern.elementAt(patternIndex)

                if (patternOpcode != null && patternOpcode.ordinal != originalOpcode.ordinal) {
                    // reaching maximum threshold (0) means,
                    // the pattern does not match to the current instructions
                    if (threshold-- == 0) break
                }

                if (patternIndex < patternLength - 1) {
                    // if the entire pattern has not been scanned yet
                    // continue the scan
                    patternIndex++
                    continue
                }
                // the pattern is valid, generate warnings if fuzzyPatternScanMethod is FuzzyPatternScanMethod
                val result =
                    MethodFingerprintResult.MethodFingerprintScanResult.PatternScanResult(index, index + patternIndex)
                if (fingerprint.fuzzyPatternScanMethod !is FuzzyPatternScanMethod) return result
                result.warnings = result.createWarnings(pattern, instructions)

                return result
            }
        }

        return null
    }
}

private typealias StringsScanResult = MethodFingerprintResult.MethodFingerprintScanResult.StringsScanResult
private typealias StringMatch = MethodFingerprintResult.MethodFingerprintScanResult.StringsScanResult.StringMatch

private fun MethodFingerprintResult.MethodFingerprintScanResult.PatternScanResult.createWarnings(
    pattern: Iterable<Opcode?>, instructions: Iterable<Instruction>
) = buildList {
    for ((patternIndex, instructionIndex) in (this@createWarnings.startIndex until this@createWarnings.endIndex).withIndex()) {
        val originalOpcode = instructions.elementAt(instructionIndex).opcode
        val patternOpcode = pattern.elementAt(patternIndex)

        if (patternOpcode == null || patternOpcode.ordinal == originalOpcode.ordinal) continue

        this.add(
            MethodFingerprintResult.MethodFingerprintScanResult.PatternScanResult.Warning(
                originalOpcode,
                patternOpcode,
                instructionIndex,
                patternIndex
            )
        )
    }
}

private operator fun ClassDef.component1() = this
private operator fun ClassDef.component2() = this.methods