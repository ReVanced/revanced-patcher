@file:Suppress("unused", "MemberVisibilityCanBePrivate")

package app.revanced.patcher

import app.revanced.patcher.extensions.instructionsOrNull
import app.revanced.patcher.patch.BytecodePatchContext
import app.revanced.patcher.patch.PatchException
import com.android.tools.smali.dexlib2.AccessFlags
import com.android.tools.smali.dexlib2.Opcode
import com.android.tools.smali.dexlib2.iface.ClassDef
import com.android.tools.smali.dexlib2.iface.Method
import com.android.tools.smali.dexlib2.iface.instruction.ReferenceInstruction
import com.android.tools.smali.dexlib2.iface.reference.StringReference
import com.android.tools.smali.dexlib2.util.MethodUtil

@Deprecated("Use the matcher API instead.")
class Fingerprint internal constructor(
    internal val accessFlags: Int?,
    internal val returnType: String?,
    internal val parameters: List<String>?,
    internal val opcodes: List<Opcode?>?,
    internal val strings: List<String>?,
    internal val custom: ((method: Method, classDef: ClassDef) -> Boolean)?,
    private val fuzzyPatternScanThreshold: Int,
) {
    @Suppress("ktlint:standard:backing-property-naming")
    // Backing field needed for lazy initialization.
    private var _matchOrNull: Match? = null

    context(_: BytecodePatchContext)
    private val matchOrNull: Match?
        get() = matchOrNull()

    context(context: BytecodePatchContext)
    internal fun matchOrNull(): Match? {
        if (_matchOrNull != null) return _matchOrNull

        var match = strings?.mapNotNull {
            context.classDefs.methodsByString[it]
        }?.minByOrNull { it.size }?.let { methodClasses ->
            methodClasses.forEach { method ->
                val match = matchOrNull(method, context.classDefs[method.definingClass]!!)
                if (match != null) return@let match
            }

            null
        }
        if (match != null) return match
        context.classDefs.forEach { classDef ->
            match = matchOrNull(classDef)
            if (match != null) return match
        }

        return null
    }

    context(_: BytecodePatchContext)
    fun matchOrNull(
        classDef: ClassDef,
    ): Match? {
        if (_matchOrNull != null) return _matchOrNull

        for (method in classDef.methods) {
            val match = matchOrNull(method, classDef)
            if (match != null) return match
        }

        return null
    }

    context(context: BytecodePatchContext)
    fun matchOrNull(
        method: Method,
    ) = matchOrNull(method, context.classDefs[method.definingClass]!!)

    context(context: BytecodePatchContext)
    fun matchOrNull(
        method: Method,
        classDef: ClassDef,
    ): Match? {
        if (_matchOrNull != null) return _matchOrNull

        if (returnType != null && !method.returnType.startsWith(returnType)) {
            return null
        }

        if (accessFlags != null && accessFlags != method.accessFlags) {
            return null
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

        // TODO: parseParameters()
        if (parameters != null && !parametersEqual(parameters, method.parameterTypes)) {
            return null
        }

        if (custom != null && !custom.invoke(method, classDef)) {
            return null
        }

        val stringMatches: List<Match.StringMatch>? =
            if (strings != null) {
                buildList {
                    val instructions = method.instructionsOrNull ?: return null

                    val stringsList = strings.toMutableList()

                    instructions.forEachIndexed { instructionIndex, instruction ->
                        if (
                            instruction.opcode != Opcode.CONST_STRING &&
                            instruction.opcode != Opcode.CONST_STRING_JUMBO
                        ) {
                            return@forEachIndexed
                        }

                        val string = ((instruction as ReferenceInstruction).reference as StringReference).string
                        val index = stringsList.indexOfFirst(string::contains)
                        if (index == -1) return@forEachIndexed

                        add(Match.StringMatch(string, instructionIndex))
                        stringsList.removeAt(index)
                    }

                    if (stringsList.isNotEmpty()) return null
                }
            } else {
                null
            }

        val patternMatch = if (opcodes != null) {
            val instructions = method.instructionsOrNull ?: return null

            fun patternScan(): Match.PatternMatch? {
                val fingerprintFuzzyPatternScanThreshold = fuzzyPatternScanThreshold

                val instructionLength = instructions.count()
                val patternLength = opcodes.size

                for (index in 0 until instructionLength) {
                    var patternIndex = 0
                    var threshold = fingerprintFuzzyPatternScanThreshold

                    while (index + patternIndex < instructionLength) {
                        val originalOpcode = instructions.elementAt(index + patternIndex).opcode
                        val patternOpcode = opcodes.elementAt(patternIndex)

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

                        // The entire pattern has been scanned.
                        return Match.PatternMatch(
                            index,
                            index + patternIndex,
                        )
                    }
                }

                return null
            }

            patternScan() ?: return null
        } else {
            null
        }

        _matchOrNull = Match(
            context,
            classDef,
            method,
            patternMatch,
            stringMatches,
        )

        return _matchOrNull
    }

    private val exception get() = PatchException("Failed to match the fingerprint: $this")

    context(_: BytecodePatchContext)
    private val match
        get() = matchOrNull ?: throw exception

    context(_: BytecodePatchContext)
    fun match(
        classDef: ClassDef,
    ) = matchOrNull(classDef) ?: throw exception

    context(_: BytecodePatchContext)
    fun match(
        method: Method,
    ) = matchOrNull(method) ?: throw exception

    context(_: BytecodePatchContext)
    fun match(
        method: Method,
        classDef: ClassDef,
    ) = matchOrNull(method, classDef) ?: throw exception

    context(_: BytecodePatchContext)
    val originalClassDefOrNull
        get() = matchOrNull?.originalClassDef

    context(_: BytecodePatchContext)
    val originalMethodOrNull
        get() = matchOrNull?.originalMethod

    context(_: BytecodePatchContext)
    val classDefOrNull
        get() = matchOrNull?.classDef

    context(_: BytecodePatchContext)
    val methodOrNull
        get() = matchOrNull?.method

    context(_: BytecodePatchContext)
    val patternMatchOrNull
        get() = matchOrNull?.patternMatch

    context(_: BytecodePatchContext)
    val stringMatchesOrNull
        get() = matchOrNull?.stringMatches

    context(_: BytecodePatchContext)
    val originalClassDef
        get() = match.originalClassDef

    context(_: BytecodePatchContext)
    val originalMethod
        get() = match.originalMethod

    context(_: BytecodePatchContext)
    val classDef
        get() = match.classDef

    context(_: BytecodePatchContext)
    val method
        get() = match.method

    context(_: BytecodePatchContext)
    val patternMatch
        get() = match.patternMatch

    context(_: BytecodePatchContext)
    val stringMatches
        get() = match.stringMatches
}

@Deprecated("Use the matcher API instead.")
class Match internal constructor(
    val context: BytecodePatchContext,
    val originalClassDef: ClassDef,
    val originalMethod: Method,
    val patternMatch: PatternMatch?,
    val stringMatches: List<StringMatch>?,
) {

    val classDef by lazy {
        val classDef = context.classDefs[originalClassDef.type]!!

        context.classDefs.getOrReplaceMutable(classDef)
    }

    val method by lazy { classDef.methods.first { MethodUtil.methodSignaturesMatch(it, originalMethod) } }

    class PatternMatch internal constructor(
        val startIndex: Int,
        val endIndex: Int,
    )

    class StringMatch internal constructor(val string: String, val index: Int)
}

@Deprecated("Use the matcher API instead.")
class FingerprintBuilder internal constructor(
    private val fuzzyPatternScanThreshold: Int = 0,
) {
    private var accessFlags: Int? = null
    private var returnType: String? = null
    private var parameters: List<String>? = null
    private var opcodes: List<Opcode?>? = null
    private var strings: List<String>? = null
    private var customBlock: ((method: Method, classDef: ClassDef) -> Boolean)? = null

    fun accessFlags(accessFlags: Int) {
        this.accessFlags = accessFlags
    }

    fun accessFlags(vararg accessFlags: AccessFlags) {
        this.accessFlags = accessFlags.fold(0) { acc, it -> acc or it.value }
    }

    fun returns(returnType: String) {
        this.returnType = returnType
    }

    fun parameters(vararg parameters: String) {
        this.parameters = parameters.toList()
    }

    fun opcodes(vararg opcodes: Opcode?) {
        this.opcodes = opcodes.toList()
    }

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

    fun strings(vararg strings: String) {
        this.strings = strings.toList()
    }

    fun custom(customBlock: (method: Method, classDef: ClassDef) -> Boolean) {
        this.customBlock = customBlock
    }

    internal fun build() = Fingerprint(
        accessFlags,
        returnType,
        parameters,
        opcodes,
        strings,
        customBlock,
        fuzzyPatternScanThreshold,
    )

    private companion object {
        val opcodesByName = Opcode.entries.associateBy { it.name }
    }
}

@Deprecated("Use the matcher API instead.")
fun fingerprint(
    fuzzyPatternScanThreshold: Int = 0,
    block: FingerprintBuilder.() -> Unit,
) = FingerprintBuilder(fuzzyPatternScanThreshold).apply(block).build()
