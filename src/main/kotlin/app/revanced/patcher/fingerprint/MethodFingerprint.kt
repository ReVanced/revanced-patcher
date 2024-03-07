package app.revanced.patcher.fingerprint

import app.revanced.patcher.data.BytecodeContext
import app.revanced.patcher.fingerprint.LookupMap.Maps.appendParameters
import app.revanced.patcher.fingerprint.LookupMap.Maps.initializeLookupMaps
import app.revanced.patcher.fingerprint.LookupMap.Maps.methodSignatureLookupMap
import app.revanced.patcher.fingerprint.LookupMap.Maps.methodStringsLookupMap
import app.revanced.patcher.fingerprint.LookupMap.Maps.methods
import app.revanced.patcher.fingerprint.MethodFingerprintResult.MethodFingerprintScanResult.StringsScanResult
import app.revanced.patcher.fingerprint.annotation.FuzzyPatternScanMethod
import app.revanced.patcher.patch.PatchException
import com.android.tools.smali.dexlib2.AccessFlags
import com.android.tools.smali.dexlib2.Opcode
import com.android.tools.smali.dexlib2.iface.ClassDef
import com.android.tools.smali.dexlib2.iface.Method
import com.android.tools.smali.dexlib2.iface.instruction.ReferenceInstruction
import com.android.tools.smali.dexlib2.iface.reference.StringReference
import kotlin.reflect.full.findAnnotations

/**
 * A fingerprint to resolve methods.
 *
 * @param returnType The method's return type compared using [String.startsWith].
 * @param accessFlags The method's exact access flags using values of [AccessFlags].
 * @param parameters The parameters of the method. Partial matches allowed and follow the same rules as [returnType].
 * @param opcodes An opcode pattern of the method's instructions. Wildcard or unknown opcodes can be specified by `null`.
 * @param strings A list of the method's strings compared each using [String.contains].
 * @param customFingerprint A custom condition for this fingerprint.
 */
@Suppress("MemberVisibilityCanBePrivate")
class MethodFingerprint(
    internal val returnType: String? = null,
    internal val accessFlags: Int? = null,
    internal val parameters: List<String>? = null,
    internal val opcodes: List<Opcode?>? = null,
    internal val strings: List<String>? = null,
    internal val customFingerprint: ((methodDef: Method, classDef: ClassDef) -> Boolean)? = null,
) {
    /**
     * The result of the [MethodFingerprint].
     */
    var result: MethodFingerprintResult? = null
        private set

    /**
     *  The [FuzzyPatternScanMethod] annotation of the [MethodFingerprint].
     *
     *  If the annotation is not present, this property is null.
     */
    val fuzzyPatternScanMethod = this::class.findAnnotations(FuzzyPatternScanMethod::class).singleOrNull()

    /**
     * Resolve a [MethodFingerprint] using the lookup map built by [initializeLookupMaps].
     *
     * [MethodFingerprint] resolution is fast, but if many are present they can consume a noticeable
     * amount of time because they are resolved in sequence.
     *
     * For apps with many fingerprints, resolving performance can be improved by:
     * - Slowest: Specify [opcodes] and nothing else.
     * - Fast: Specify [accessFlags], [returnType].
     * - Faster: Specify [accessFlags], [returnType] and [parameters].
     * - Fastest: Specify [strings], with at least one string being an exact (non-partial) match.
     */
    internal fun resolveUsingLookupMap(context: BytecodeContext): Boolean {
        /**
         * Lookup [MethodClassPair]s that match the methods strings present in a [MethodFingerprint].
         *
         * @return A list of [MethodClassPair]s that match the methods strings present in a [MethodFingerprint].
         */
        fun MethodFingerprint.methodStringsLookup(): LookupMap.MethodClassList? {
            strings?.forEach {
                val methods = methodStringsLookupMap[it]
                if (methods != null) return methods
            }
            return null
        }

        /**
         * Lookup [MethodClassPair]s that match the method signature present in a [MethodFingerprint].
         *
         * @return A list of [MethodClassPair]s that match the method signature present in a [MethodFingerprint].
         */
        fun MethodFingerprint.methodSignatureLookup(): LookupMap.MethodClassList {
            if (accessFlags == null) return methods

            var returnTypeValue = returnType
            if (returnTypeValue == null) {
                if (AccessFlags.CONSTRUCTOR.isSet(accessFlags)) {
                    // Constructors always have void return type
                    returnTypeValue = "V"
                } else {
                    return methods
                }
            }

            val key =
                buildString {
                    append(accessFlags)
                    append(returnTypeValue.first())
                    if (parameters != null) appendParameters(parameters)
                }
            return methodSignatureLookupMap[key] ?: return LookupMap.MethodClassList()
        }

        /**
         * Resolve a [MethodFingerprint] using a list of [MethodClassPair].
         *
         * @return True if the resolution was successful, false otherwise.
         */
        fun MethodFingerprint.resolveUsingMethodClassPair(methodClasses: LookupMap.MethodClassList): Boolean {
            methodClasses.forEach { classAndMethod ->
                if (resolve(context, classAndMethod.first, classAndMethod.second)) return true
            }
            return false
        }

        val methodsWithSameStrings = methodStringsLookup()
        if (methodsWithSameStrings != null) {
            if (resolveUsingMethodClassPair(methodsWithSameStrings)) {
                return true
            }
        }

        // No strings declared or none matched (partial matches are allowed).
        // Use signature matching.
        return resolveUsingMethodClassPair(methodSignatureLookup())
    }

    /**
     * Resolve a [MethodFingerprint] against a [ClassDef].
     *
     * @param forClass The class on which to resolve the [MethodFingerprint] in.
     * @param context The [BytecodeContext] to host proxies.
     * @return True if the resolution was successful, false otherwise.
     */
    fun resolve(
        context: BytecodeContext,
        forClass: ClassDef,
    ): Boolean {
        for (method in forClass.methods)
            if (resolve(context, method, forClass)) {
                return true
            }
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
    fun resolve(
        context: BytecodeContext,
        method: Method,
        forClass: ClassDef,
    ): Boolean {
        val methodFingerprint = this

        if (methodFingerprint.result != null) return true

        if (methodFingerprint.returnType != null && !method.returnType.startsWith(methodFingerprint.returnType)) {
            return false
        }

        if (methodFingerprint.accessFlags != null && methodFingerprint.accessFlags != method.accessFlags) {
            return false
        }

        fun parametersEqual(
            parameters1: Iterable<CharSequence>,
            parameters2: Iterable<CharSequence>,
        ): Boolean {
            if (parameters1.count() != parameters2.count()) return false
            val iterator1 = parameters1.iterator()
            parameters2.forEach {
                if (!it.startsWith(iterator1.next())) return false
            }
            return true
        }

        if (methodFingerprint.parameters != null &&
            !parametersEqual(
                // TODO: parseParameters()
                methodFingerprint.parameters,
                method.parameterTypes,
            )
        ) {
            return false
        }

        @Suppress("UNNECESSARY_NOT_NULL_ASSERTION")
        if (methodFingerprint.customFingerprint != null && !methodFingerprint.customFingerprint!!(method, forClass)) {
            return false
        }

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
                            ) {
                                return@forEachIndexed
                            }

                            val string = ((instruction as ReferenceInstruction).reference as StringReference).string
                            val index = stringsList.indexOfFirst(string::contains)
                            if (index == -1) return@forEachIndexed

                            add(StringsScanResult.StringMatch(string, instructionIndex))
                            stringsList.removeAt(index)
                        }

                        if (stringsList.isNotEmpty()) return false
                    },
                )
            } else {
                null
            }

        val patternScanResult =
            if (methodFingerprint.opcodes != null) {
                method.implementation?.instructions ?: return false

                fun Method.patternScan(
                    fingerprint: MethodFingerprint,
                ): MethodFingerprintResult.MethodFingerprintScanResult.PatternScanResult? {
                    val instructions = this.implementation!!.instructions
                    val fingerprintFuzzyPatternScanThreshold = fingerprint.fuzzyPatternScanMethod?.threshold ?: 0

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
                                // If the entire pattern has not been scanned yet, continue the scan.
                                patternIndex++
                                continue
                            }
                            // The pattern is valid.
                            return MethodFingerprintResult.MethodFingerprintScanResult.PatternScanResult(
                                index,
                                index + patternIndex,
                            )
                        }
                    }

                    return null
                }

                method.patternScan(methodFingerprint) ?: return false
            } else {
                null
            }

        methodFingerprint.result =
            MethodFingerprintResult(
                method,
                forClass,
                MethodFingerprintResult.MethodFingerprintScanResult(
                    patternScanResult,
                    stringsScanResult,
                ),
                context,
            )

        return true
    }

    companion object {
        /**
         * Resolve a list of [MethodFingerprint] using the lookup map built by [initializeLookupMaps].
         *
         * [MethodFingerprint] resolution is fast, but if many are present they can consume a noticeable
         * amount of time because they are resolved in sequence.
         *
         * For apps with many fingerprints, resolving performance can be improved by:
         * - Slowest: Specify [opcodes] and nothing else.
         * - Fast: Specify [accessFlags], [returnType].
         * - Faster: Specify [accessFlags], [returnType] and [parameters].
         * - Fastest: Specify [strings], with at least one string being an exact (non-partial) match.
         */
        internal fun Set<MethodFingerprint>.resolveUsingLookupMap(context: BytecodeContext) {
            if (methods.isEmpty()) throw PatchException("lookup map not initialized")

            forEach { fingerprint ->
                fingerprint.resolveUsingLookupMap(context)
            }
        }

        /**
         * Resolve a list of [MethodFingerprint] against a list of [ClassDef].
         *
         * @param classes The classes on which to resolve the [MethodFingerprint] in.
         * @param context The [BytecodeContext] to host proxies.
         * @return True if the resolution was successful, false otherwise.
         */
        fun Iterable<MethodFingerprint>.resolve(
            context: BytecodeContext,
            classes: Iterable<ClassDef>,
        ) = forEach { fingerprint ->
            for (classDef in classes) {
                if (fingerprint.resolve(context, classDef)) break
            }
        }
    }
}
