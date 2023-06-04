package app.revanced.patcher.fingerprint.method.impl

import app.revanced.patcher.data.BytecodeContext
import app.revanced.patcher.extensions.MethodFingerprintExtensions.fuzzyPatternScanMethod
import app.revanced.patcher.extensions.MethodFingerprintExtensions.fuzzyScanThreshold
import app.revanced.patcher.extensions.MethodFingerprintExtensions.name
import app.revanced.patcher.extensions.parametersEqual
import app.revanced.patcher.fingerprint.Fingerprint
import app.revanced.patcher.fingerprint.method.annotation.FuzzyPatternScanMethod
import app.revanced.patcher.logging.Logger
import app.revanced.patcher.logging.impl.NopLogger
import app.revanced.patcher.patch.PatchResultError
import app.revanced.patcher.util.proxy.ClassProxy
import org.jf.dexlib2.AccessFlags
import org.jf.dexlib2.Opcode
import org.jf.dexlib2.iface.ClassDef
import org.jf.dexlib2.iface.Method
import org.jf.dexlib2.iface.instruction.Instruction
import org.jf.dexlib2.iface.instruction.ReferenceInstruction
import org.jf.dexlib2.iface.reference.StringReference
import org.jf.dexlib2.util.MethodUtil
import java.util.LinkedList

private typealias StringMatch = MethodFingerprintResult.MethodFingerprintScanResult.StringsScanResult.StringMatch
private typealias StringsScanResult = MethodFingerprintResult.MethodFingerprintScanResult.StringsScanResult

/**
 * A finger print for identifying a method.
 *
 * To improve patching performance:
 * - fastest: specify at least [strings], with the first string being an exact (non-partial) match.
 * - faster: specify at least [accessFlags], [returnType], [parameters].
 * - fast: specify at least [accessFlags], [returnType].
 * - slowest: specify only [opcodes] and nothing else.
 *
 * For target apps with only a few patches, the resolving speed does not matter and even the
 * slowest resolving fingerprints will not be noticed. Only when using dozens of fingerprints
 * does the patching speed become apparent.
 *
 * @param returnType The return type of the method. Partial matches are allowed, and values are compared using startWith.
 *                   For example: "L" matches any object, while "Landroid/view/View;" matches only to an Android view parameter.
 * @param accessFlags The access flags of the method using values of [AccessFlags].  Must be an exact match.
 * @param parameters The parameters of the method. Partial matches allowed and follow same rules as [returnType].
 * @param opcodes The list of opcodes of the method, and a `null` opcode means unknown or wildcard value.
 * @param strings A list of strings which a method contains.
 *                Partial matches are allowed, and are compared using contains. For example, "app" matches "happy".
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

        private class ClassAndMethod(val classDef: ClassDef, val method: Method)
        /**
         * All methods in the target app.
         */
        private val allMethods = mutableListOf<ClassAndMethod>()
        /**
         * Map of all methods in the target app, keyed to the access/return/parameter signature.
         */
        private val signatureMap = mutableMapOf<String, MutableList<ClassAndMethod>>()
        /**
         * Map of all Strings found in the target app, and the class/method they were found in.
         */
        private val stringMap = mutableMapOf<String, MutableList<ClassAndMethod>>()

        private fun StringBuilder.appendSignatureKeyParameters(parameters: Iterable<CharSequence>) {
            // Maximum parameters to use in the signature key.
            // Used to reduce the map size by grouping together uncommon methods.
            val maxSignatureParameters = 5
            append("p:")
            parameters.forEachIndexed { index, parameter ->
                if (index >= maxSignatureParameters) return
                append(parameter.first())
            }
        }

        /**
         * @return all app methods that contain the first string declared in this signature,
         *         or NULL if no strings are declared or no exact matches exist.
         */
        private fun MethodFingerprint.appMethodsWithSameStrings() : List<ClassAndMethod>? {
            if (strings != null && strings.count() > 0) {
                // Only check the first String declared
                return stringMap[strings.first()]
            }
            return null
        }

        /**
         * @return all app methods that could match this signature.
         */
        private fun MethodFingerprint.appMethodsWithSameSignature() : List<ClassAndMethod> {
            if (accessFlags == null) return allMethods

            var returnTypeValue = returnType
            if (returnTypeValue == null) {
                if (AccessFlags.CONSTRUCTOR.isSet(accessFlags)) {
                    // Constructors always have void return type
                    returnTypeValue = "V"
                } else {
                    return allMethods
                }
            }

            val key = buildString {
                append(accessFlags)
                append(returnTypeValue.first())
                if (parameters != null) appendSignatureKeyParameters(parameters)
            }
            return signatureMap[key]!!
        }

        internal fun clearMethodLookupMap() {
            allMethods.clear()
            signatureMap.clear()
            stringMap.clear()
        }

        /**
         * resolve faster, by creating culled lists based on method signature and Strings contained.
         */
        internal fun createMethodLookupMap(classes: Iterable<ClassDef>) {
            fun addMethodToMapList(map: MutableMap<String, MutableList<ClassAndMethod>>,
                                   key: String, keyValue:  ClassAndMethod) {
                var list = map[key]
                if (list == null) {
                    list = LinkedList()
                    map[key] = list
                }
                list += keyValue
            }

            for (classDef in classes) {
                for (method in classDef.methods) {
                    // Key structure is: (access)(returnType)(optional: parameter types)
                    val accessFlagsReturnKey = method.accessFlags.toString() + method.returnType.first()
                    val accessFlagsReturnParametersKey = buildString {
                        append(accessFlagsReturnKey)
                        appendSignatureKeyParameters(method.parameterTypes)
                    }
                    val classAndMethod = ClassAndMethod(classDef, method)

                    // For signatures with no access or return type specified.
                    allMethods += classAndMethod
                    // Access and return type.
                    addMethodToMapList(signatureMap, accessFlagsReturnKey, classAndMethod)
                    // Access, return, and parameters.
                    addMethodToMapList(signatureMap, accessFlagsReturnParametersKey, classAndMethod)

                    // Look up by Strings (the fastest way to resolve).
                    method.implementation?.instructions?.forEach { instruction ->
                        if (instruction.opcode == Opcode.CONST_STRING || instruction.opcode == Opcode.CONST_STRING_JUMBO) {
                            val string = ((instruction as ReferenceInstruction).reference as StringReference).string
                            addMethodToMapList(stringMap, string, classAndMethod)
                        }
                    }

                    // The only additional lookup that could benefit, is a map of the full class name to its methods.
                    // This would require adding a 'class name' field to MethodFingerprint,
                    // as currently the class name can be specified only with a custom fingerprint.
                }
            }
        }

        /**
         * Resolve using map built in [createMethodLookupMap]
         */
        internal fun Iterable<MethodFingerprint>.resolveUsingLookupMap(context: BytecodeContext, logger : Logger = NopLogger) {
            if (allMethods.isEmpty()) throw PatchResultError("lookup map not initialized")

            for (fingerprint in this) {
                var time = System.currentTimeMillis()
                fingerprint.resolveUsingLookupMap(context, logger)
                time = System.currentTimeMillis() - time
                if (time > 20) logger.trace("${fingerprint.name} resolved in $time ms")
            }
        }

        /**
         * Resolve a list of [MethodFingerprint] against a list of [ClassDef].
         *
         * @param classes The classes on which to resolve the [MethodFingerprint] in.
         * @param context The [BytecodeContext] to host proxies.
         * @return True if the resolution was successful, false otherwise.
         */
        fun Iterable<MethodFingerprint>.resolve(context: BytecodeContext, classes: Iterable<ClassDef>) {
            for (fingerprint in this) // For each fingerprint
                classes@ for (classDef in classes) // search through all classes for the fingerprint
                    if (fingerprint.resolve(context, classDef))
                        break@classes // if the resolution succeeded, continue with the next fingerprint
        }

        /**
         * Resolve using map built in [createMethodLookupMap]
         *
         * @param logger optional logger, to record the time to resolve each fingerprint.
         */
        internal fun MethodFingerprint.resolveUsingLookupMap(context: BytecodeContext, logger : Logger = NopLogger): Boolean {
            fun MethodFingerprint.resolveUsingClassMethod(classMethods: Iterable<ClassAndMethod>): Boolean {
                for (classAndMethod in classMethods) {
                    if (resolve(context, classAndMethod.method, classAndMethod.classDef)) {
                        return true
                    }
                }
                return false
            }

            var methodsWithStrings = appMethodsWithSameStrings()
            if (methodsWithStrings != null) {
                if (resolveUsingClassMethod(methodsWithStrings)) return true
                logger.trace("$name: could not quickly resolve using declared strings (verify first string is an exact match)")
            }

            // No String declared, or none matched (partial matches are allowed).
            // Use signature matching.
            return resolveUsingClassMethod(appMethodsWithSameSignature())
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
    val mutableClass by lazy { context.proxy(classDef).mutableClass }

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