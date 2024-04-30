@file:Suppress("unused", "MemberVisibilityCanBePrivate")

package app.revanced.patcher.fingerprint

import app.revanced.patcher.fingerprint.LookupMap.Maps.appendParameters
import app.revanced.patcher.fingerprint.LookupMap.Maps.initializeLookupMaps
import app.revanced.patcher.fingerprint.LookupMap.Maps.methodSignatureLookupMap
import app.revanced.patcher.fingerprint.LookupMap.Maps.methodStringsLookupMap
import app.revanced.patcher.fingerprint.LookupMap.Maps.methods
import app.revanced.patcher.fingerprint.MethodFingerprintResult.MethodFingerprintScanResult.StringsScanResult
import app.revanced.patcher.patch.*
import app.revanced.patcher.util.proxy.ClassProxy
import com.android.tools.smali.dexlib2.AccessFlags
import com.android.tools.smali.dexlib2.Opcode
import com.android.tools.smali.dexlib2.iface.ClassDef
import com.android.tools.smali.dexlib2.iface.Method
import com.android.tools.smali.dexlib2.iface.instruction.ReferenceInstruction
import com.android.tools.smali.dexlib2.iface.reference.StringReference
import com.android.tools.smali.dexlib2.util.MethodUtil

/**
 * A fingerprint to resolve methods.
 *
 * @param accessFlags The exact access flags using values of [AccessFlags].
 * @param returnType The return type compared using [String.startsWith].
 * @param parameters The parameters of the method. Partial matches allowed and follow the same rules as [returnType].
 * @param opcodes An opcode pattern of the instructions. Wildcard or unknown opcodes can be specified by `null`.
 * @param strings A list of the strings compared each using [String.contains].
 * @param custom A custom condition for this fingerprint.
 * @param fuzzyPatternScanThreshold The threshold for fuzzy pattern scanning.
 */
@Suppress("MemberVisibilityCanBePrivate")
class MethodFingerprint internal constructor(
    internal val accessFlags: Int? = null,
    internal val returnType: String? = null,
    internal val parameters: List<String>? = null,
    internal val opcodes: List<Opcode?>? = null,
    internal val strings: List<String>? = null,
    internal val custom: ((methodDef: Method, classDef: ClassDef) -> Boolean)? = null,
    private val fuzzyPatternScanThreshold: Int = 0,
) {
    /**
     * The result of the [MethodFingerprint].
     */
    var result: MethodFingerprintResult? = null
        private set

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
    internal fun resolveUsingLookupMap(context: BytecodePatchContext): Boolean {
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
     * @param context The [BytecodePatchContext] to host proxies.
     * @return True if the resolution was successful, false otherwise.
     */
    fun resolve(
        context: BytecodePatchContext,
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
     * @param context The [BytecodePatchContext] to host proxies.
     * @return True if the resolution was successful or if the fingerprint is already resolved, false otherwise.
     */
    fun resolve(
        context: BytecodePatchContext,
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
        if (methodFingerprint.custom != null && !methodFingerprint.custom!!(method, forClass)) {
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
                    val fingerprintFuzzyPatternScanThreshold = fingerprint.fuzzyPatternScanThreshold

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
}

/**
 * Resolve a list of [MethodFingerprint] using the lookup map built by [initializeLookupMaps].
 *
 * [MethodFingerprint] resolution is fast, but if many are present they can consume a noticeable
 * amount of time because they are resolved in sequence.
 *
 * For apps with many fingerprints, resolving performance can be improved by:
 * - Slowest: Specify [MethodFingerprint.opcodes] and nothing else.
 * - Fast: Specify [MethodFingerprint.accessFlags], [MethodFingerprint.returnType].
 * - Faster: Specify [MethodFingerprint.accessFlags], [MethodFingerprint.returnType] and [MethodFingerprint.parameters].
 * - Fastest: Specify [MethodFingerprint.strings], with at least one string being an exact (non-partial) match.
 */
internal fun Set<MethodFingerprint>.resolveUsingLookupMap(context: BytecodePatchContext) = forEach { fingerprint ->
    fingerprint.resolveUsingLookupMap(context)
}

/**
 * Resolve a list of [MethodFingerprint] against a list of [ClassDef].
 *
 * @param classes The classes on which to resolve the [MethodFingerprint] in.
 * @param context The [BytecodePatchContext] to host proxies.
 * @return True if the resolution was successful, false otherwise.
 */
fun Iterable<MethodFingerprint>.resolve(
    context: BytecodePatchContext,
    classes: Iterable<ClassDef>,
) = forEach { fingerprint ->
    for (classDef in classes) {
        if (fingerprint.resolve(context, classDef)) break
    }
}

/**
 * Represents the result of a [MethodFingerprintResult].
 *
 * @param method The matching method.
 * @param classDef The [ClassDef] that contains the matching [method].
 * @param scanResult The result of scanning for the [MethodFingerprint].
 * @param context The [BytecodePatchContext] this [MethodFingerprintResult] is attached to, to create proxies.
 */

class MethodFingerprintResult(
    val method: Method,
    val classDef: ClassDef,
    val scanResult: MethodFingerprintScanResult,
    internal val context: BytecodePatchContext,
) {
    /**
     * Returns a mutable clone of [classDef]
     *
     * Please note, this method allocates a [ClassProxy].
     * Use [classDef] where possible.
     */
    val mutableClass by lazy {
        context.proxy(classDef).mutableClass
    }

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
    class MethodFingerprintScanResult(
        val patternScanResult: PatternScanResult?,
        val stringsScanResult: StringsScanResult?,
    ) {
        /**
         * The result of scanning strings on the [MethodFingerprint].
         * @param matches The list of strings that were matched.
         */
        class StringsScanResult(val matches: List<StringMatch>) {
            /**
             * Represents a match for a string at an index.
             * @param string The string that was matched.
             * @param index The index of the string.
             */
            class StringMatch(val string: String, val index: Int)
        }

        /**
         * The result of a pattern scan.
         * @param startIndex The start index of the instructions where to which this pattern matches.
         * @param endIndex The end index of the instructions where to which this pattern matches.
         */
        class PatternScanResult(
            val startIndex: Int,
            val endIndex: Int,
        )
    }
}

/**
 * A builder for [MethodFingerprint].
 *
 * @property accessFlags The exact access flags using values of [AccessFlags].
 * @property returnType The return type compared using [String.startsWith].
 * @property parameters The parameters of the method. Partial matches allowed and follow the same rules as [returnType].
 * @property opcodes An opcode pattern of the instructions. Wildcard or unknown opcodes can be specified by `null`.
 * @property strings A list of the strings compared each using [String.contains].
 * @property customBlock A custom condition for this fingerprint.
 * @property fuzzyPatternScanThreshold The threshold for fuzzy pattern scanning.
 *
 * @constructor Create a new [MethodFingerprintBuilder].
 */
class MethodFingerprintBuilder internal constructor(
    private val fuzzyPatternScanThreshold: Int = 0,
) {
    private var accessFlags: Int? = null
    private var returnType: String? = null
    private var parameters: List<String>? = null
    private var opcodes: List<Opcode?>? = null
    private var strings: List<String>? = null
    private var customBlock: ((methodDef: Method, classDef: ClassDef) -> Boolean)? = null

    /**
     * Set the access flags.
     *
     * @param accessFlags The exact access flags using values of [AccessFlags].
     */
    fun accessFlags(accessFlags: Int) {
        this.accessFlags = accessFlags
    }

    /**
     * Set the access flags.
     *
     * @param accessFlags The exact access flags using values of [AccessFlags].
     */
    fun accessFlags(vararg accessFlags: AccessFlags) {
        this.accessFlags = accessFlags.fold(0) { acc, it -> acc or it.value }
    }

    /**
     * Set the return type.
     *
     * @param returnType The return type compared using [String.startsWith].
     */
    infix fun returns(returnType: String) {
        this.returnType = returnType
    }

    /**
     * Set the parameters.
     *
     * @param parameters The parameters of the method. Partial matches allowed and follow the same rules as [returnType].
     */
    fun parameters(vararg parameters: String) {
        this.parameters = parameters.toList()
    }

    /**
     * Set the opcodes.
     *
     * @param opcodes An opcode pattern of instructions.
     * Wildcard or unknown opcodes can be specified by `null`.
     */
    fun opcodes(vararg opcodes: Opcode?) {
        this.opcodes = opcodes.toList()
    }

    /**
     * Set the opcodes.
     *
     * @param instructions A list of instructions or opcode names in SMALI format.
     * - Wildcard or unknown opcodes can be specified by `null`.
     * - Empty lines are ignored.
     * - Each instruction must be on a new line.
     * - The opcode name is enough, no need to specify the operands.
     *
     * @throws Exception If an unknown opcode is used.
     */
    fun opcodes(instructions: String) {
        this.opcodes = instructions.trimIndent().split("\n").filter {
            it.isNotBlank()
        }.map {
            // Remove any operands.
            val name = it.split(" ", limit = 1).first().trim()
            if (name == "null") return@map null

            opcodesByName[name] ?: throw Exception("Unknown opcode: $name")
        }
    }

    /**
     * Set the strings.
     *
     * @param strings A list of strings compared each using [String.contains].
     */
    fun strings(vararg strings: String) {
        this.strings = strings.toList()
    }

    /**
     * Set a custom condition for this fingerprint.
     *
     * @param customBlock A custom condition for this fingerprint.
     */
    fun custom(customBlock: (methodDef: Method, classDef: ClassDef) -> Boolean) {
        this.customBlock = customBlock
    }

    internal fun build() = MethodFingerprint(accessFlags, returnType, parameters, opcodes, strings, customBlock)

    private companion object {
        val opcodesByName = Opcode.entries.associateBy { it.name }
    }
}

/**
 * Create a [MethodFingerprint].
 *
 * @param fuzzyPatternScanThreshold The threshold for fuzzy pattern scanning. Default is 0.
 * @param block The block to build the [MethodFingerprint].
 *
 * @return The created [MethodFingerprint].
 */
fun methodFingerprint(
    fuzzyPatternScanThreshold: Int = 0,
    block: MethodFingerprintBuilder.() -> Unit,
) = MethodFingerprintBuilder(fuzzyPatternScanThreshold).apply(block).build()

/**
 * Create a [MethodFingerprint] and add it to the set of fingerprints.
 *
 * @param fuzzyPatternScanThreshold The threshold for fuzzy pattern scanning. Default is 0.
 * @param block The block to build the [MethodFingerprint].
 *
 * @return The created [MethodFingerprint].
 */
fun BytecodePatchBuilder.methodFingerprint(
    fuzzyPatternScanThreshold: Int = 0,
    block: MethodFingerprintBuilder.() -> Unit,
) = app.revanced.patcher.fingerprint.methodFingerprint(
    fuzzyPatternScanThreshold,
    block,
)() // Invoke to add to its set of fingerprints.
