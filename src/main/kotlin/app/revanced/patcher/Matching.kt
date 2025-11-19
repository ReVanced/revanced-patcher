@file:Suppress("unused", "MemberVisibilityCanBePrivate", "CONTEXT_RECEIVERS_DEPRECATED")

package app.revanced.patcher

import app.revanced.patcher.Matcher.MatchContext
import app.revanced.patcher.dex.mutable.MutableMethod
import app.revanced.patcher.patch.BytecodePatchContext
import com.android.tools.smali.dexlib2.HiddenApiRestriction
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
) = lookupMaps.classesByType[type]?.takeIf {
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

fun BytecodePatchContext.firstMethodOrNull(predicate: MatchPredicate<Method>) = with(predicate) {
    with(MatchContext()) {
        classDefs.asSequence().flatMap { it.methods.asSequence() }.firstOrNull { it.match() }
    }
}

fun BytecodePatchContext.firstMethod(predicate: MatchPredicate<Method>) = requireNotNull(firstMethodOrNull(predicate))

fun BytecodePatchContext.firstMethodMutableOrNull(predicate: MatchPredicate<Method>): MutableMethod? {
    with(predicate) {
        with(MatchContext()) {
            classDefs.forEach { classDef ->
                classDef.methods.firstOrNull { it.match() }?.let { method ->
                    return classDef.mutable().methods.first { MethodUtil.methodSignaturesMatch(it, method) }
                }
            }
        }
    }

    return null
}

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

    class MatchContext internal constructor() : MutableMap<String, Any> by mutableMapOf()
}


fun <T> slidingWindowMatcher(builder: MutableList<T.() -> Boolean>.() -> Unit) =
    SlidingWindowMatcher<T>().apply(builder)

context(MatchContext)
fun <T> Iterable<T>.matchSlidingWindow(key: String, builder: MutableList<T.() -> Boolean>.() -> Unit) =
    (getOrPut(key) { slidingWindowMatcher(builder) } as Matcher<T, T.() -> Boolean>)(this)

fun <T> Iterable<T>.matchSlidingWindow(builder: MutableList<T.() -> Boolean>.() -> Unit) =
    slidingWindowMatcher(builder)(this)

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

fun findStringsMatcher(builder: MutableList<String>.() -> Unit) =
    FindStringsMatcher().apply(builder)

class FindStringsMatcher() : Matcher<Instruction, String>() {
    val matchedStrings = mutableMapOf<String, Int>()
    var needles = toMutableSet() // Reduce O(n²) to O(log n) by removing from the set

    override fun invoke(haystack: Iterable<Instruction>): Boolean {
        needles = toMutableSet() // Reset needles for each invocation
        // (or do not use the set if set is too small for performance)

        val foundStrings = mutableMapOf<String, Int>()

        haystack.forEachIndexed { index, instruction ->
            if (instruction !is ReferenceInstruction) return@forEachIndexed
            val reference = instruction.reference
            if (reference !is StringReference) return@forEachIndexed
            val string = reference.string

            if (needles.removeIf { it in string }) {
                foundStrings[string] = index
            }
        }

        return if (foundStrings.size == size) {
            matchedStrings += foundStrings

            true
        } else {
            false
        }
    }
}

fun BytecodePatchContext.findStringIndices() {
    val match = findStringsMatcher {
        add("fullstring1")
        add("fullstring2")
        add("partialString")
    }

    firstMethod("fullstring", "fullstring") {
        implementation {
            match(instructions)
        }
    }

    match.matchedStrings.forEach { (key, value) ->
        println("Found string '$key' at index $value")
    }

    firstMethod {
        implementation {
            // Uncached usage
            instructions.matchSlidingWindow {

            } || instructions.matchSlidingWindow("cached usage") {

            }
        }
    }
}

fun BytecodePatchContext.anotherExample() {
    val desiredStringIndices = listOf("fullstring1", "fullstring2", "partialString")
    val matchedIndices = mutableMapOf<String, Int>()

    firstMethod("fullstring", "fullstring") {
        val remaining = desiredStringIndices.toMutableSet()
        val foundMap = mutableMapOf<String, Int>()

        implementation {

            instructions.withIndex().forEach { (index, instruction) ->
                val string = (instruction as? ReferenceInstruction)?.reference
                    .let { it as? StringReference }?.string
                    ?: return@forEach

                val iterator = remaining.iterator()
                while (iterator.hasNext()) {
                    val desired = iterator.next()
                    if (desired in string) {
                        foundMap[desired] = index
                        iterator.remove()
                    }
                }

                if (remaining.isEmpty()) return@forEach
            }

            if (remaining.isEmpty()) {
                matchedIndices.putAll(foundMap)
                true
            } else {
                false
            }
        }
    }
}

fun BytecodePatchContext.wrapperExample() {
    fun Method.captureStrings(
        desiredStringIndices: Set<String>,
        out: MutableMap<String, Int>
    ): Boolean {
        val remaining = desiredStringIndices.toMutableSet()
        val foundMap = mutableMapOf<String, Int>()

        return implementation {
            instructions.withIndex().forEach { (index, instruction) ->
                val string = (instruction as? ReferenceInstruction)?.reference
                    .let { it as? StringReference }?.string
                    ?: return@forEach

                val iterator = remaining.iterator()
                while (iterator.hasNext()) {
                    val desired = iterator.next()
                    if (desired in string) {
                        foundMap[desired] = index
                        iterator.remove()
                    }
                }

                if (remaining.isEmpty()) return@forEach
            }

            if (remaining.isEmpty()) {
                out += foundMap
                true
            } else {
                false
            }
        }
    }

    val desiredStringIndices = setOf("fullstring1", "fullstring2", "partialString")
    val matchedIndices = mutableMapOf<String, Int>()

    val method = firstMethod {
        name == "desiredMethodName" && captureStrings(desiredStringIndices, matchedIndices)
    }

    for ((key, value) in matchedIndices) {
        println("Found string '$key' at index $value in method '$method'")
    }
}
