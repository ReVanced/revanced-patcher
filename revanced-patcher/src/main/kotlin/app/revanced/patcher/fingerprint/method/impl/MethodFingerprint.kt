package app.revanced.patcher.fingerprint.method.impl

import app.revanced.patcher.BytecodeContext
import app.revanced.patcher.extensions.MethodFingerprintExtensions.fuzzyPatternScanMethod
import app.revanced.patcher.fingerprint.Fingerprint
import app.revanced.patcher.fingerprint.method.annotation.FuzzyPatternScanMethod
import app.revanced.patcher.patch.PatchException
import app.revanced.patcher.util.proxy.ClassProxy
import org.jf.dexlib2.AccessFlags
import org.jf.dexlib2.Opcode
import org.jf.dexlib2.iface.ClassDef
import org.jf.dexlib2.iface.Method
import org.jf.dexlib2.iface.instruction.Instruction
import org.jf.dexlib2.iface.instruction.ReferenceInstruction
import org.jf.dexlib2.iface.reference.StringReference
import org.jf.dexlib2.util.MethodUtil
import java.util.*

private typealias StringMatch = MethodFingerprintResult.MethodFingerprintScanResult.StringsScanResult.StringMatch
private typealias StringsScanResult = MethodFingerprintResult.MethodFingerprintScanResult.StringsScanResult
private typealias MethodClassPair = Pair<Method, ClassDef>

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
abstract class MethodFingerprint(
    internal val returnType: String? = null,
    internal val accessFlags: Int? = null,
    internal val parameters: Iterable<String>? = null,
    internal val opcodes: Iterable<Opcode?>? = null,
    internal val strings: Iterable<String>? = null,
    internal val customFingerprint: ((methodDef: Method, classDef: ClassDef) -> Boolean)? = null
) : Fingerprint {
    /**
     * The result of the [MethodFingerprint].
     */
    var result: MethodFingerprintResult? = null

    companion object {
        /**
         * A list of methods and the class they were found in.
         */
        private val methods = mutableListOf<MethodClassPair>()

        /**
         * Lookup map for methods keyed to the methods access flags, return type and parameter.
         */
        private val methodSignatureLookupMap = mutableMapOf<String, MutableList<MethodClassPair>>()

        /**
         * Lookup map for methods keyed to the strings contained in the method.
         */
        private val methodStringsLookupMap = mutableMapOf<String, MutableList<MethodClassPair>>()

        /**
         * Appends a string based on the parameter reference types of this method.
         */
        private fun StringBuilder.appendParameters(parameters: Iterable<CharSequence>) {
            // Maximum parameters to use in the signature key.
            // Some apps have methods with an incredible number of parameters (over 100 parameters have been seen).
            // To keep the signature map from becoming needlessly bloated,
            // group together in the same map entry all methods with the same access/return and 5 or more parameters.
            // The value of 5 was chosen based on local performance testing and is not set in stone.
            val maxSignatureParameters = 5
            // Must append a unique value before the parameters to distinguish this key includes the parameters.
            // If this is not appended, then methods with no parameters
            // will collide with different keys that specify access/return but omit the parameters.
            append("p:")
            parameters.forEachIndexed { index, parameter ->
                if (index >= maxSignatureParameters) return
                append(parameter.first())
            }
        }

        /**
         * Initializes lookup maps for [MethodFingerprint] resolution
         * using attributes of methods such as the method signature or strings.
         *
         * @param context The [BytecodeContext] containing the classes to initialize the lookup maps with.
         */
        internal fun initializeFingerprintResolutionLookupMaps(context: BytecodeContext) {
            fun MutableMap<String, MutableList<MethodClassPair>>.add(
                key: String,
                methodClassPair: MethodClassPair
            ) {
                var methodClassPairs = this[key]

                methodClassPairs ?: run {
                    methodClassPairs = LinkedList<MethodClassPair>().also { this[key] = it }
                }

                methodClassPairs!!.add(methodClassPair)
            }

            if (methods.isNotEmpty()) throw PatchException("Map already initialized")

            context.classes.forEach { classDef ->
                classDef.methods.forEach { method ->
                    val methodClassPair = method to classDef

                    // For fingerprints with no access or return type specified.
                    methods += methodClassPair

                    val accessFlagsReturnKey = method.accessFlags.toString() + method.returnType.first()

                    // Add <access><returnType> as the key.
                    methodSignatureLookupMap.add(accessFlagsReturnKey, methodClassPair)

                    // Add <access><returnType>[parameters] as the key.
                    methodSignatureLookupMap.add(
                        buildString {
                            append(accessFlagsReturnKey)
                            appendParameters(method.parameterTypes)
                        },
                        methodClassPair
                    )

                    // Add strings contained in the method as the key.
                    method.implementation?.instructions?.forEach instructions@{ instruction ->
                        if (instruction.opcode != Opcode.CONST_STRING && instruction.opcode != Opcode.CONST_STRING_JUMBO)
                            return@instructions

                        val string = ((instruction as ReferenceInstruction).reference as StringReference).string

                        methodStringsLookupMap.add(string, methodClassPair)
                    }

                    // In the future, the class type could be added to the lookup map.
                    // This would require MethodFingerprint to be changed to include the class type.
                }
            }
        }

        /**
         * Clears the internal lookup maps created in [initializeFingerprintResolutionLookupMaps]
         */
        internal fun clearFingerprintResolutionLookupMaps() {
            methods.clear()
            methodSignatureLookupMap.clear()
            methodStringsLookupMap.clear()
        }

        /**
         * Resolve a list of [MethodFingerprint] using the lookup map built by [initializeFingerprintResolutionLookupMaps].
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
        internal fun Iterable<MethodFingerprint>.resolveUsingLookupMap(context: BytecodeContext) {
            if (methods.isEmpty()) throw PatchException("lookup map not initialized")

            for (fingerprint in this) {
                fingerprint.resolveUsingLookupMap(context)
            }
        }

        /**
         * Resolve a [MethodFingerprint] using the lookup map built by [initializeFingerprintResolutionLookupMaps].
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
        internal fun MethodFingerprint.resolveUsingLookupMap(context: BytecodeContext): Boolean {
            /**
             * Lookup [MethodClassPair]s that match the methods strings present in a [MethodFingerprint].
             *
             * @return A list of [MethodClassPair]s that match the methods strings present in a [MethodFingerprint].
             */
            fun MethodFingerprint.methodStringsLookup(): List<MethodClassPair>? {
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
            fun MethodFingerprint.methodSignatureLookup(): List<MethodClassPair> {
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

                val key = buildString {
                    append(accessFlags)
                    append(returnTypeValue.first())
                    if (parameters != null) appendParameters(parameters)
                }
                return methodSignatureLookupMap[key] ?: return emptyList()
            }

            /**
             * Resolve a [MethodFingerprint] using a list of [MethodClassPair].
             *
             * @return True if the resolution was successful, false otherwise.
             */
            fun MethodFingerprint.resolveUsingMethodClassPair(classMethods: Iterable<MethodClassPair>): Boolean {
                classMethods.forEach { classAndMethod ->
                    if (resolve(context, classAndMethod.first, classAndMethod.second)) return true
                }
                return false
            }

            val methodsWithSameStrings = methodStringsLookup()
            if (methodsWithSameStrings != null) if (resolveUsingMethodClassPair(methodsWithSameStrings)) return true

            // No strings declared or none matched (partial matches are allowed).
            // Use signature matching.
            return resolveUsingMethodClassPair(methodSignatureLookup())
        }

        /**
         * Resolve a list of [MethodFingerprint] against a list of [ClassDef].
         *
         * @param classes The classes on which to resolve the [MethodFingerprint] in.
         * @param context The [BytecodeContext] to host proxies.
         * @return True if the resolution was successful, false otherwise.
         */
        fun Iterable<MethodFingerprint>.resolve(context: BytecodeContext, classes: Iterable<ClassDef>) {
            for (fingerprint in this) // For each fingerprint...
                classes@ for (classDef in classes) // ...search through all classes for the MethodFingerprint
                    if (fingerprint.resolve(context, classDef))
                        break@classes // ...if the resolution succeeded, continue with the next MethodFingerprint.
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

            if (methodFingerprint.accessFlags != null && methodFingerprint.accessFlags != method.accessFlags)
                return false


            fun parametersEqual(
                parameters1: Iterable<CharSequence>, parameters2: Iterable<CharSequence>
            ): Boolean {
                if (parameters1.count() != parameters2.count()) return false
                val iterator1 = parameters1.iterator()
                parameters2.forEach {
                    if (!it.startsWith(iterator1.next())) return false
                }
                return true
            }

            if (methodFingerprint.parameters != null && !parametersEqual(
                    methodFingerprint.parameters, // TODO: parseParameters()
                    method.parameterTypes
                )
            ) return false

            @Suppress("UNNECESSARY_NOT_NULL_ASSERTION")
            if (methodFingerprint.customFingerprint != null && !methodFingerprint.customFingerprint!!(method, forClass))
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

                fun Method.patternScan(
                    fingerprint: MethodFingerprint
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
                                // If the entire pattern has not been scanned yet
                                // continue the scan.
                                patternIndex++
                                continue
                            }
                            // The pattern is valid, generate warnings if fuzzyPatternScanMethod is FuzzyPatternScanMethod
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
}