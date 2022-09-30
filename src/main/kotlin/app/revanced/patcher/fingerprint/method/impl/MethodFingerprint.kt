package app.revanced.patcher.fingerprint.method.impl

import app.revanced.patcher.BytecodeContext
import app.revanced.patcher.extensions.MethodFingerprintExtensions.fuzzyPatternScanMethod
import app.revanced.patcher.extensions.MethodFingerprintExtensions.fuzzyScanThreshold
import app.revanced.patcher.extensions.parametersEqual
import app.revanced.patcher.fingerprint.Fingerprint
import app.revanced.patcher.fingerprint.method.annotation.FuzzyPatternScanMethod
import app.revanced.patcher.util.proxy.ClassProxy
import org.jf.dexlib2.Opcode
import org.jf.dexlib2.iface.ClassDef
import org.jf.dexlib2.iface.Method
import org.jf.dexlib2.iface.instruction.Instruction
import org.jf.dexlib2.iface.instruction.ReferenceInstruction
import org.jf.dexlib2.iface.reference.StringReference
import org.jf.dexlib2.util.MethodUtil

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

    companion object {
        /**
         * Resolve a list of [MethodFingerprint] against a list of [ClassDef].
         *
         * @param classes The classes on which to resolve the [MethodFingerprint] in.
         * @param context The [BytecodeContext] to host proxies.
         * @return True if the resolution was successful, false otherwise.
         */
        fun Iterable<MethodFingerprint>.resolve(context: BytecodeContext, classes: Iterable<ClassDef>) {
            for (fingerprint in this)
                classes@ for (classDef in classes)
                    if (fingerprint.resolve(context, classDef))
                        break@classes // If the resolution succeeded, continue with the next fingerprint.
        }

        /**
         * Resolve a [MethodFingerprint] against a [ClassDef].
         *
         * @param forClass The class on which to resolve the [MethodFingerprint] in.
         * @param context The [BytecodeContext] to host proxies.
         * @return True if the resolution was successful, false otherwise.
         */
        fun MethodFingerprint.resolve(context: BytecodeContext, forClass: ClassDef): Boolean {
            for (method in forClass.methods)
                if (this.resolve(context, method, forClass))
                    return true
            return false
        }

        /**
         * Resolve a [MethodFingerprint] against a [Method].
         *
         * @param method The class on which to resolve the [MethodFingerprint] in.
         * @param forClass The class on which to resolve the [MethodFingerprint].
         * @param context The [BytecodeContext] to host proxies.
         * @return True if the resolution was successful or if the fingerprint is already resolved, false otherwise.
         */
        fun MethodFingerprint.resolve(context: BytecodeContext, method: Method, forClass: ClassDef): Boolean {
            val methodFingerprint = this

            if (methodFingerprint.result != null) return true

            if (methodFingerprint.returnType != null && !method.returnType.startsWith(methodFingerprint.returnType))
                return false

            if (methodFingerprint.access != null && methodFingerprint.access != method.accessFlags)
                return false


            if (methodFingerprint.parameters != null && !parametersEqual(
                    methodFingerprint.parameters, // TODO: parseParameters()
                    method.parameterTypes
                )
            ) return false

            @Suppress("UNNECESSARY_NOT_NULL_ASSERTION")
            if (methodFingerprint.customFingerprint != null && !methodFingerprint.customFingerprint!!(method))
                return false

            val stringsScanResult: StringsScanResult? =
                if (methodFingerprint.strings != null) {
                    StringsScanResult(
                        buildList {
                            val implementation = method.implementation ?: return false

                            val stringsList = methodFingerprint.strings.toMutableList()

                            implementation.instructions.forEachIndexed { instructionIndex, instruction ->
                                if (
                                    instruction.opcode != Opcode.CONST_STRING &&
                                    instruction.opcode != Opcode.CONST_STRING_JUMBO
                                ) return@forEachIndexed

                                val string = ((instruction as ReferenceInstruction).reference as StringReference).string
                                val index = stringsList.indexOfFirst(string::contains)
                                if (index == -1) return@forEachIndexed

                                add(
                                    StringMatch(
                                        string,
                                        instructionIndex
                                    )
                                )
                                stringsList.removeAt(index)
                            }

                            if (stringsList.isNotEmpty()) return false
                        }
                    )
                } else null

            val patternScanResult = if (methodFingerprint.opcodes != null) {
                method.implementation?.instructions ?: return false

                method.patternScan(methodFingerprint) ?: return false
            } else null

            methodFingerprint.result = MethodFingerprintResult(
                method,
                forClass,
                MethodFingerprintResult.MethodFingerprintScanResult(
                    patternScanResult,
                    stringsScanResult
                ),
                context
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
                        // Reaching maximum threshold (0) means,
                        // the pattern does not match to the current instructions.
                        if (threshold-- == 0) break
                    }

                    if (patternIndex < patternLength - 1) {
                        // If the entire pattern has not been scanned yet
                        // continue the scan.
                        patternIndex++
                        continue
                    }
                    // The pattern is valid, generate warnings if fuzzyPatternScanMethod is FuzzyPatternScanMethod.
                    val result =
                        MethodFingerprintResult.MethodFingerprintScanResult.PatternScanResult(
                            index,
                            index + patternIndex
                        )
                    if (fingerprint.fuzzyPatternScanMethod !is FuzzyPatternScanMethod) return result
                    result.warnings = result.createWarnings(pattern, instructions)

                    return result
                }
            }

            return null
        }

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
    }
}

private typealias StringMatch = MethodFingerprintResult.MethodFingerprintScanResult.StringsScanResult.StringMatch
private typealias StringsScanResult = MethodFingerprintResult.MethodFingerprintScanResult.StringsScanResult

/**
 * Represents the result of a [MethodFingerprintResult].
 *
 * @param method The matching method.
 * @param classDef The [ClassDef] that contains the matching [method].
 * @param scanResult The result of scanning for the [MethodFingerprint].
 * @param context The [BytecodeContext] this [MethodFingerprintResult] is attached to, to create proxies.
 */
data class MethodFingerprintResult(
    val method: Method,
    val classDef: ClassDef,
    val scanResult: MethodFingerprintScanResult,
    internal val context: BytecodeContext
) {

    /**
     * The result of scanning on the [MethodFingerprint].
     * @param patternScanResult The result of the pattern scan.
     * @param stringsScanResult The result of the string scan.
     */
    data class MethodFingerprintScanResult(
        val patternScanResult: PatternScanResult?,
        val stringsScanResult: StringsScanResult?
    ) {
        /**
         * The result of scanning strings on the [MethodFingerprint].
         * @param matches The list of strings that were matched.
         */
        data class StringsScanResult(val matches: List<StringMatch>) {
            /**
             * Represents a match for a string at an index.
             * @param string The string that was matched.
             * @param index The index of the string.
             */
            data class StringMatch(val string: String, val index: Int)
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
    }

    /**
     * Returns a mutable clone of [classDef]
     *
     * Please note, this method allocates a [ClassProxy].
     * Use [classDef] where possible.
     */
    @Suppress("MemberVisibilityCanBePrivate")
    val mutableClass by lazy { context.classes.proxy(classDef).mutableClass }

    /**
     * Returns a mutable clone of [method]
     *
     * Please note, this method allocates a [ClassProxy].
     * Use [method] where possible.
     */
    val mutableMethod by lazy {
        mutableClass.methods.first {
            MethodUtil.methodSignaturesMatch(it, this.method)
        }
    }
}