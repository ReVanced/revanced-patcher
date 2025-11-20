@file:Suppress("unused", "MemberVisibilityCanBePrivate", "CONTEXT_RECEIVERS_DEPRECATED")

package app.revanced.patcher

import app.revanced.patcher.Matcher.MatchContext
import app.revanced.patcher.dex.mutable.MutableMethod
import app.revanced.patcher.extensions.instructions
import app.revanced.patcher.patch.BytecodePatchContext
import com.android.tools.smali.dexlib2.HiddenApiRestriction
import com.android.tools.smali.dexlib2.Opcode
import com.android.tools.smali.dexlib2.iface.*
import com.android.tools.smali.dexlib2.iface.Annotation
import com.android.tools.smali.dexlib2.iface.instruction.Instruction
import com.android.tools.smali.dexlib2.iface.instruction.ReferenceInstruction
import com.android.tools.smali.dexlib2.iface.reference.StringReference
import com.android.tools.smali.dexlib2.util.MethodUtil
import kotlin.properties.ReadOnlyProperty
import kotlin.reflect.KProperty

fun Iterable<ClassDef>.anyClassDef(predicate: ClassDef.() -> Boolean) = any(predicate)

fun ClassDef.anyMethod(predicate: Method.() -> Boolean) = methods.any(predicate)

fun ClassDef.anyDirectMethod(predicate: Method.() -> Boolean) = directMethods.any(predicate)

fun ClassDef.anyVirtualMethod(predicate: Method.() -> Boolean) = virtualMethods.any(predicate)

fun ClassDef.anyField(predicate: Field.() -> Boolean) = fields.any(predicate)

fun ClassDef.anyInstanceField(predicate: Field.() -> Boolean) = instanceFields.any(predicate)

fun ClassDef.anyStaticField(predicate: Field.() -> Boolean) = staticFields.any(predicate)

fun ClassDef.anyInterface(predicate: String.() -> Boolean) = interfaces.any(predicate)

fun ClassDef.anyAnnotation(predicate: Annotation.() -> Boolean) = annotations.any(predicate)

fun Method.implementation(predicate: MethodImplementation.() -> Boolean) = implementation?.predicate() ?: false

fun Method.anyParameter(predicate: MethodParameter.() -> Boolean) = parameters.any(predicate)

fun Method.anyParameterType(predicate: CharSequence.() -> Boolean) = parameterTypes.any(predicate)

fun Method.anyAnnotation(predicate: Annotation.() -> Boolean) = annotations.any(predicate)

fun Method.anyHiddenApiRestriction(predicate: HiddenApiRestriction.() -> Boolean) = hiddenApiRestrictions.any(predicate)

fun MethodImplementation.anyInstruction(predicate: Instruction.() -> Boolean) = instructions.any(predicate)

fun MethodImplementation.anyTryBlock(predicate: TryBlock<out ExceptionHandler>.() -> Boolean) = tryBlocks.any(predicate)

fun MethodImplementation.anyDebugItem(predicate: Any.() -> Boolean) = debugItems.any(predicate)

fun Iterable<Instruction>.anyInstruction(predicate: Instruction.() -> Boolean) = any(predicate)

fun BytecodePatchContext.firstClassDefOrNull(predicate: MatchPredicate<ClassDef>) =
    with(predicate) { with(MatchContext()) { classDefs.firstOrNull { it.match() } } }

fun BytecodePatchContext.firstClassDef(predicate: MatchPredicate<ClassDef>) =
    requireNotNull(firstClassDefOrNull(predicate))

fun BytecodePatchContext.firstClassDefMutableOrNull(predicate: MatchPredicate<ClassDef>) =
    firstClassDefOrNull(predicate)?.mutable()

fun BytecodePatchContext.firstClassDefMutable(predicate: MatchPredicate<ClassDef>) =
    requireNotNull(firstClassDefMutableOrNull(predicate))

fun BytecodePatchContext.firstClassDefOrNull(
    type: String, predicate: (MatchPredicate<ClassDef>)? = null
) = lookupMaps.classDefsByType[type]?.takeIf {
    predicate == null || with(predicate) { with(MatchContext()) { it.match() } }
}

fun BytecodePatchContext.firstClassDef(
    type: String, predicate: (MatchPredicate<ClassDef>)? = null
) = requireNotNull(firstClassDefOrNull(type, predicate))

fun BytecodePatchContext.firstClassDefMutableOrNull(
    type: String, predicate: (MatchPredicate<ClassDef>)? = null
) = firstClassDefOrNull(type, predicate)?.mutable()

fun BytecodePatchContext.firstClassDefMutable(
    type: String, predicate: (MatchPredicate<ClassDef>)? = null
) = requireNotNull(firstClassDefMutableOrNull(type, predicate))

fun Iterable<ClassDef>.firstMethodOrNull(predicate: MatchPredicate<Method>) = with(predicate) {
    with(MatchContext()) {
        this@firstMethodOrNull.asSequence().flatMap { it.methods.asSequence() }.firstOrNull { it.match() }
    }
}

fun Iterable<ClassDef>.firstMethod(predicate: MatchPredicate<Method>) = requireNotNull(firstMethodOrNull(predicate))

context(BytecodePatchContext)
fun Iterable<ClassDef>.firstMethodMutableOrNull(predicate: MatchPredicate<Method>): MutableMethod? {
    with(predicate) {
        with(MatchContext()) {
            this@firstMethodMutableOrNull.forEach { classDef ->
                classDef.methods.firstOrNull { it.match() }?.let { method ->
                    return classDef.mutable().methods.first { MethodUtil.methodSignaturesMatch(it, method) }
                }
            }
        }
    }

    return null
}

context(BytecodePatchContext)
fun Iterable<ClassDef>.firstMethodMutable(predicate: MatchPredicate<Method>) =
    requireNotNull(firstMethodMutableOrNull(predicate))

fun BytecodePatchContext.firstMethodOrNull(predicate: MatchPredicate<Method>) =
    classDefs.firstMethodOrNull(predicate)

fun BytecodePatchContext.firstMethod(predicate: MatchPredicate<Method>) =
    requireNotNull(firstMethodOrNull(predicate))


fun BytecodePatchContext.firstMethodMutableOrNull(predicate: MatchPredicate<Method>) =
    classDefs.firstMethodMutableOrNull(predicate)

fun BytecodePatchContext.firstMethodMutable(predicate: MatchPredicate<Method>) =
    requireNotNull(firstMethodMutableOrNull(predicate))

fun BytecodePatchContext.firstMethodOrNull(
    vararg strings: String,
    predicate: MatchPredicate<Method> = MatchPredicate { true },
) = with(predicate) {
    with(MatchContext()) {
        strings.mapNotNull { lookupMaps.methodsByStrings[it] }.minByOrNull { it.size }?.firstOrNull { it.match() }
    }
}

fun BytecodePatchContext.firstMethod(
    vararg strings: String,
    predicate: MatchPredicate<Method> = MatchPredicate { true },
) = requireNotNull(firstMethodOrNull(*strings, predicate = predicate))

fun BytecodePatchContext.firstMethodMutableOrNull(
    vararg strings: String,
    predicate: MatchPredicate<Method> = MatchPredicate { true },
) = with(predicate) {
    with(MatchContext()) {
        strings.mapNotNull { lookupMaps.methodsByStrings[it] }.minByOrNull { it.size }?.let { methods ->
            methods.firstOrNull { it.match() }?.let { method ->
                firstClassDefMutable(method.definingClass).methods.first {
                    MethodUtil.methodSignaturesMatch(
                        method, it
                    )
                }
            }
        }
    }
}

fun BytecodePatchContext.firstMethodMutable(
    vararg strings: String, predicate: MatchPredicate<Method> = MatchPredicate { true }
) = requireNotNull(firstMethodMutableOrNull(*strings, predicate = predicate))

inline fun <reified C, T> ReadOnlyProperty(crossinline block: C.(KProperty<*>) -> T) =
    ReadOnlyProperty<Any?, T> { thisRef, property ->
        require(thisRef is C)

        thisRef.block(property)
    }

fun gettingFirstClassDefOrNull(predicate: MatchPredicate<ClassDef>) =
    ReadOnlyProperty<BytecodePatchContext, ClassDef?> { firstClassDefOrNull(predicate) }

fun gettingFirstClassDef(predicate: MatchPredicate<ClassDef>) = requireNotNull(gettingFirstClassDefOrNull(predicate))

fun gettingFirstClassDefMutableOrNull(predicate: MatchPredicate<ClassDef>) =
    ReadOnlyProperty<BytecodePatchContext, ClassDef?> { firstClassDefMutableOrNull(predicate) }

fun gettingFirstClassDefMutable(predicate: MatchPredicate<ClassDef>) =
    requireNotNull(gettingFirstClassDefMutableOrNull(predicate))

fun gettingFirstClassDefOrNull(
    type: String, predicate: (MatchPredicate<ClassDef>)? = null
) = ReadOnlyProperty<BytecodePatchContext, ClassDef?> { firstClassDefOrNull(type, predicate) }

fun gettingFirstClassDef(
    type: String, predicate: (MatchPredicate<ClassDef>)? = null
) = requireNotNull(gettingFirstClassDefOrNull(type, predicate))

fun gettingFirstClassDefMutableOrNull(
    type: String, predicate: (MatchPredicate<ClassDef>)? = null
) = ReadOnlyProperty<BytecodePatchContext, ClassDef?> { firstClassDefMutableOrNull(type, predicate) }

fun gettingFirstClassDefMutable(
    type: String, predicate: (MatchPredicate<ClassDef>)? = null
) = requireNotNull(gettingFirstClassDefMutableOrNull(type, predicate))

fun gettingFirstMethodOrNull(predicate: MatchPredicate<Method>) =
    ReadOnlyProperty<BytecodePatchContext, Method?> { firstMethodOrNull(predicate) }

fun gettingFirstMethod(predicate: MatchPredicate<Method>) = requireNotNull(gettingFirstMethodOrNull(predicate))

fun gettingFirstMethodMutableOrNull(predicate: MatchPredicate<Method>) =
    ReadOnlyProperty<BytecodePatchContext, Method?> { firstMethodMutableOrNull(predicate) }

fun gettingFirstMethodMutable(predicate: MatchPredicate<Method>) =
    requireNotNull(gettingFirstMethodMutableOrNull(predicate))

fun gettingFirstMethodOrNull(
    vararg strings: String,
    predicate: MatchPredicate<Method> = MatchPredicate { true },
) = ReadOnlyProperty<BytecodePatchContext, Method?> { firstMethodOrNull(*strings, predicate = predicate) }

fun gettingFirstMethod(
    vararg strings: String,
    predicate: MatchPredicate<Method> = MatchPredicate { true },
) = requireNotNull(gettingFirstMethodOrNull(*strings, predicate = predicate))

fun gettingFirstMethodMutableOrNull(
    vararg strings: String,
    predicate: MatchPredicate<Method> = MatchPredicate { true },
) = ReadOnlyProperty<BytecodePatchContext, Method?> { firstMethodMutableOrNull(*strings, predicate = predicate) }

fun gettingFirstMethodMutable(
    vararg strings: String,
    predicate: MatchPredicate<Method> = MatchPredicate { true },
) = requireNotNull(gettingFirstMethodMutableOrNull(*strings, predicate = predicate))

fun interface MatchPredicate<T> {
    context(MatchContext) fun T.match(): Boolean
}

abstract class Matcher<T, U> : MutableList<U> by mutableListOf() {
    var matchIndex = -1
        protected set

    abstract operator fun invoke(haystack: Iterable<T>): Boolean

    class MatchContext internal constructor() : MutableMap<String, Any> by mutableMapOf() {
        inline fun <reified V : Any> remember(key: String, defaultValue: () -> V) =
            get(key) as? V ?: defaultValue().also { put(key, it) }
    }
}

fun <T> slidingWindowMatcher(build: SlidingWindowMatcher<T>.() -> Unit) =
    SlidingWindowMatcher<T>().apply(build)

context(MatchContext)
fun <T> Iterable<T>.matchSlidingWindow(key: String, build: SlidingWindowMatcher<T>.() -> Unit) =
    remember(key) { slidingWindowMatcher(build) }(this)

fun <T> Iterable<T>.matchSlidingWindow(build: SlidingWindowMatcher<T>.() -> Unit) =
    slidingWindowMatcher(build)(this)

class SlidingWindowMatcher<T>() : Matcher<T, T.() -> Boolean>() {
    override operator fun invoke(haystack: Iterable<T>): Boolean {
        val haystackCount = haystack.count()
        val needleSize = size
        if (needleSize == 0) return false

        for (i in 0..(haystackCount - needleSize)) {
            var matched = true
            for (j in 0 until needleSize) {
                if (!this[j].invoke(haystack.elementAt(i + j))) {
                    matched = false
                    break
                }
            }
            if (matched) {
                matchIndex = i
                return true
            }
        }

        matchIndex = -1
        return false
    }
}

fun findStringsMatcher(build: MutableList<String>.() -> Unit) =
    FindStringsMatcher().apply(build)

class FindStringsMatcher() : Matcher<Instruction, String>() {
    private val _matchedStrings = mutableListOf<Pair<String, Int>>()
    val matchedStrings: List<Pair<String, Int>> = _matchedStrings

    override fun invoke(haystack: Iterable<Instruction>): Boolean {
        _matchedStrings.clear()
        val remaining = indices.toMutableList()

        haystack.forEachIndexed { hayIndex, instruction ->
            val string = ((instruction as? ReferenceInstruction)?.reference as? StringReference)?.string
                ?: return@forEachIndexed

            val index = remaining.firstOrNull { this[it] in string } ?: return@forEachIndexed

            _matchedStrings += this[index] to hayIndex
            remaining -= index
        }

        return remaining.isEmpty()
    }
}

fun BytecodePatchContext.a() {
    val match = indexedMatcher<Instruction> {
        first { opcode == Opcode.OR_INT_2ADDR }
        after { opcode == Opcode.RETURN_VOID }
        after(atLeast = 2, atMost = 5) { opcode == Opcode.MOVE_RESULT_OBJECT }
        opcode(Opcode.RETURN_VOID)
    }

    val myMethod = firstMethod {
        implementation { match(instructions) }
    }

    match._indices // Mapped in same order as defined
}

fun IndexedMatcher<Instruction>.opcode(opcode: Opcode) {
    after { this.opcode == opcode }
}

context(MatchContext)
fun Method.instructions(key: String, build: IndexedMatcher<Instruction>.() -> Unit) =
    instructions.matchIndexed("instructions", build)


fun <T> indexedMatcher(build: IndexedMatcher<T>.() -> Unit) =
    IndexedMatcher<T>().apply(build)

context(MatchContext)
fun <T> Iterable<T>.matchIndexed(key: String, build: IndexedMatcher<T>.() -> Unit) =
    remember<IndexedMatcher<T>>(key) { indexedMatcher(build) }(this)

class IndexedMatcher<T>() : Matcher<T, T.() -> Boolean>() {
    private val _indices: MutableList<Int> = mutableListOf()
    val indices: List<Int> = _indices

    private var lastMatchedIndex = -1
    private var currentIndex = -1

    override fun invoke(haystack: Iterable<T>): Boolean {
        val hayList = haystack as? List<T> ?: haystack.toList()

        _indices.clear()

        var firstNeedleIndex = 0

        while (firstNeedleIndex <= hayList.lastIndex) {
            lastMatchedIndex = -1

            val tempIndices = mutableListOf<Int>()

            var matchedAll = true
            var subIndex = firstNeedleIndex

            for (predicateIndex in _indices.indices) {
                var predicateMatched = false

                while (subIndex <= hayList.lastIndex) {
                    currentIndex = subIndex
                    val element = hayList[subIndex]
                    if (this[predicateIndex](element)) {
                        tempIndices.add(subIndex)
                        lastMatchedIndex = subIndex
                        predicateMatched = true
                        subIndex++
                        break
                    }
                    subIndex++
                }

                if (!predicateMatched) {
                    // Restart from next possible first match
                    firstNeedleIndex = if (tempIndices.isNotEmpty()) tempIndices[0] + 1 else firstNeedleIndex + 1
                    matchedAll = false
                    break
                }
            }

            if (matchedAll) {
                _indices.addAll(tempIndices)
                return true
            }
        }

        return false
    }

    fun first(predicate: T.() -> Boolean) = add {
        if (lastMatchedIndex != -1) false
        else predicate()
    }

    fun after(atLeast: Int = 1, atMost: Int = 1, predicate: T.() -> Boolean) = add {
        val distance = currentIndex - lastMatchedIndex
        if (distance in atLeast..atMost) predicate() else false
    }
}
