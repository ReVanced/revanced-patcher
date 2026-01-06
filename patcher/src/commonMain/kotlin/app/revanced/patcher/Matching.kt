@file:Suppress("unused")

package app.revanced.patcher

import app.revanced.patcher.BytecodePatchContextClassDefMatching.firstClassDefOrNull
import app.revanced.patcher.BytecodePatchContextClassDefMatching.firstMutableClassDefOrNull
import app.revanced.patcher.BytecodePatchContextMethodMatching.firstMutableMethod
import app.revanced.patcher.BytecodePatchContextMethodMatching.firstMutableMethodOrNull
import app.revanced.patcher.BytecodePatchContextMethodMatching.gettingFirstMethodDeclarativelyOrNull
import app.revanced.patcher.ClassDefMethodMatching.firstMethodDeclarativelyOrNull
import app.revanced.patcher.IterableClassDefClassDefMatching.firstClassDefOrNull
import app.revanced.patcher.IterableClassDefMethodMatching.firstMethodOrNull
import app.revanced.patcher.IterableMethodMethodMatching.firstMethodDeclarativelyOrNull
import app.revanced.patcher.IterableMethodMethodMatching.firstMethodOrNull
import app.revanced.patcher.IterableMethodMethodMatching.firstMutableMethodOrNull
import app.revanced.patcher.extensions.*
import app.revanced.patcher.patch.BytecodePatchContext
import com.android.tools.smali.dexlib2.AccessFlags
import com.android.tools.smali.dexlib2.HiddenApiRestriction
import com.android.tools.smali.dexlib2.Opcode
import com.android.tools.smali.dexlib2.iface.*
import com.android.tools.smali.dexlib2.iface.Annotation
import com.android.tools.smali.dexlib2.iface.instruction.*
import com.android.tools.smali.dexlib2.iface.reference.MethodReference
import com.android.tools.smali.dexlib2.mutable.MutableMethod
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

private typealias ClassDefPredicate = context(PredicateContext) ClassDef.() -> Boolean

private typealias MethodPredicate = context(PredicateContext) Method.() -> Boolean


inline fun <reified V> PredicateContext.remember(key: Any, defaultValue: () -> V) = if (key in this) get(key) as V
else defaultValue().also { put(key, it) }

private fun <T> cachedReadOnlyProperty(block: BytecodePatchContext.(KProperty<*>) -> T) =
    JVMConflict.cachedReadOnlyProperty(block)

private object JVMConflict {
    fun <R, T> cachedReadOnlyProperty(block: R.(KProperty<*>) -> T) = object : ReadOnlyProperty<R, T?> {
        private val cache = HashMap<R, T>(1)

        override fun getValue(thisRef: R, property: KProperty<*>) =
            (if (thisRef in cache) cache[thisRef] else cache.getOrPut(thisRef) { thisRef.block(property) })
    }
}

class MutablePredicateList<T> internal constructor() : MutableList<T.() -> Boolean> by mutableListOf()

private typealias DeclarativePredicate<T> = context(PredicateContext) MutablePredicateList<T>.() -> Unit


fun <T> T.declarativePredicate(build: MutablePredicateList<T>.() -> Unit) =
    context(MutablePredicateList<T>().apply(build)) {
        all(this)
    }

context(context: PredicateContext)
fun <T> T.rememberDeclarativePredicate(key: Any, block: MutablePredicateList<T>.() -> Unit) =
    context(context.remember(key) { MutablePredicateList<T>().apply(block) }) {
        all(this)
    }

context(_: PredicateContext)
private fun <T> T.rememberDeclarativePredicate(
    predicate: DeclarativePredicate<T>
) = rememberDeclarativePredicate("declarativePredicate") { predicate() }

object IterableMethodMethodMatching {
    fun Iterable<Method>.firstMethodOrNull(
        methodReference: MethodReference
    ) = firstOrNull { MethodUtil.methodSignaturesMatch(methodReference, it) }

    fun Iterable<Method>.firstMethod(
        methodReference: MethodReference
    ) = requireNotNull(firstMethodOrNull(methodReference))

    context(context: BytecodePatchContext)
    fun Iterable<Method>.firstMutableMethodOrNull(
        methodReference: MethodReference
    ) = firstMethodOrNull(methodReference)?.let { context.firstMutableMethod(it) }

    context(_: BytecodePatchContext)
    fun Iterable<Method>.firstMutableMethod(
        methodReference: MethodReference
    ) = requireNotNull(firstMutableMethodOrNull(methodReference))

    fun Iterable<Method>.firstMethodOrNull(
        vararg strings: String,
        predicate: MethodPredicate = { true },
    ) = if (strings.isEmpty()) withPredicateContext { firstOrNull { it.predicate() } }
    else withPredicateContext {
        first { method ->
            val instructions = method.instructionsOrNull ?: return@first false

            // TODO: Check potential to optimize (Set or not).
            //  Maybe even use context maps, but the methods may not be present in the context yet.
            val methodStrings = instructions.asSequence().mapNotNull { it.string }.toSet()

            if (strings.any { it !in methodStrings }) return@first false

            method.predicate()
        }
    }

    fun Iterable<Method>.firstMethod(
        vararg strings: String,
        predicate: MethodPredicate = { true },
    ) = requireNotNull(firstMethodOrNull(strings = strings, predicate))

    context(context: BytecodePatchContext)
    fun Iterable<Method>.firstMutableMethodOrNull(
        vararg strings: String,
        predicate: MethodPredicate = { true },
    ) = firstMethodOrNull(strings = strings, predicate)?.let { context.firstMutableMethod(it) }

    context(_: BytecodePatchContext)
    fun Iterable<Method>.firstMutableMethod(
        vararg strings: String,
        predicate: MethodPredicate = { true },
    ) = requireNotNull(firstMutableMethodOrNull(strings = strings, predicate))

    fun Iterable<Method>.firstMethodDeclarativelyOrNull(
        predicate: DeclarativePredicate<Method>
    ) = firstMethodOrNull { rememberDeclarativePredicate(predicate) }

    fun Iterable<Method>.firstMethodDeclaratively(
        predicate: DeclarativePredicate<Method>
    ) = requireNotNull(firstMethodDeclarativelyOrNull(predicate))
}

object IterableClassDefMethodMatching {
    fun Iterable<ClassDef>.firstMethodOrNull(
        methodReference: MethodReference
    ) = asSequence().flatMap { it.methods.asSequence() }.asIterable().firstMethodOrNull(methodReference)

    fun Iterable<ClassDef>.firstMethod(
        methodReference: MethodReference
    ) = requireNotNull(firstMethodOrNull(methodReference))

    context(context: BytecodePatchContext)
    fun Iterable<ClassDef>.firstMutableMethodOrNull(
        methodReference: MethodReference
    ) = asSequence().flatMap { it.methods.asSequence() }.asIterable().firstMutableMethodOrNull(methodReference)

    context(_: BytecodePatchContext)
    fun Iterable<ClassDef>.firstMutableMethod(
        methodReference: MethodReference
    ) = requireNotNull(firstMutableMethodOrNull(methodReference))

    fun Iterable<ClassDef>.firstMethodOrNull(
        predicate: MethodPredicate = { true },
    ) = asSequence().flatMap { it.methods.asSequence() }.asIterable()
        .firstMethodOrNull(strings = emptyArray(), predicate)

    fun Iterable<ClassDef>.firstMethod(
        predicate: MethodPredicate = { true },
    ) = requireNotNull(firstMethodOrNull(predicate))

    fun Iterable<ClassDef>.firstMethodOrNull(
        vararg strings: String,
        predicate: MethodPredicate = { true },
    ) = asSequence().flatMap { it.methods.asSequence() }.asIterable().firstMethodOrNull(strings = strings, predicate)

    fun Iterable<ClassDef>.firstMethod(
        vararg strings: String,
        predicate: MethodPredicate = { true },
    ) = requireNotNull(firstMethodOrNull(strings = strings, predicate))

    context(context: BytecodePatchContext)
    fun Iterable<ClassDef>.firstMutableMethodOrNull(
        vararg strings: String,
        predicate: MethodPredicate = { true },
    ) = firstMethodOrNull(strings = strings, predicate)?.let { context.firstMutableMethod(it) }

    context(context: BytecodePatchContext)
    fun Iterable<ClassDef>.firstMutableMethod(
        vararg strings: String,
        predicate: MethodPredicate = { true },
    ) = requireNotNull(firstMutableMethodOrNull(strings = strings, predicate))

    fun Iterable<ClassDef>.firstMethodDeclarativelyOrNull(
        predicate: DeclarativePredicate<Method>
    ) = firstMethodOrNull { rememberDeclarativePredicate(predicate) }

    fun Iterable<ClassDef>.firstMethodDeclaratively(
        predicate: DeclarativePredicate<Method>
    ) = requireNotNull(firstMethodDeclarativelyOrNull(predicate))

    context(context: BytecodePatchContext)
    fun Iterable<ClassDef>.firstMethodDeclarativelyOrNull(
        vararg strings: String,
        predicate: DeclarativePredicate<Method>
    ) = firstMethodOrNull(strings = strings) { rememberDeclarativePredicate(predicate) }

    context(context: BytecodePatchContext)
    fun Iterable<ClassDef>.firstMethodDeclaratively(
        vararg strings: String,
        predicate: DeclarativePredicate<Method>
    ) = requireNotNull(firstMethodDeclarativelyOrNull(strings = strings, predicate))

    context(context: BytecodePatchContext)
    fun Iterable<ClassDef>.firstMutableMethodDeclarativelyOrNull(
        vararg strings: String,
        predicate: DeclarativePredicate<Method>
    ) = firstMutableMethodOrNull(strings = strings) { rememberDeclarativePredicate(predicate) }
}

object ClassDefMethodMatching {
    fun ClassDef.firstMethodOrNull(
        methodReference: MethodReference
    ) = methods.firstMethodOrNull(methodReference)

    fun ClassDef.firstMethod(
        methodReference: MethodReference
    ) = requireNotNull(firstMethodOrNull(methodReference))

    context(_: BytecodePatchContext)
    fun ClassDef.firstMutableMethodOrNull(
        methodReference: MethodReference
    ) = methods.firstMutableMethodOrNull(methodReference)

    context(_: BytecodePatchContext)
    fun ClassDef.firstMutableMethod(
        methodReference: MethodReference
    ) = requireNotNull(firstMutableMethodOrNull(methodReference))

    fun ClassDef.firstMethodOrNull(
        predicate: MethodPredicate = { true },
    ) = methods.firstMethodOrNull(strings = emptyArray(), predicate)

    fun ClassDef.firstMethod(
        predicate: MethodPredicate = { true },
    ) = requireNotNull(firstMethodOrNull(predicate))

    fun ClassDef.firstMethodOrNull(
        vararg strings: String,
        predicate: MethodPredicate = { true },
    ) = methods.firstMethodOrNull(strings = strings, predicate)

    fun ClassDef.firstMethod(
        vararg strings: String,
        predicate: MethodPredicate = { true },
    ) = requireNotNull(firstMethodOrNull(strings = strings, predicate))

    fun ClassDef.firstMethodDeclarativelyOrNull(
        predicate: DeclarativePredicate<Method>
    ) = methods.firstMethodDeclarativelyOrNull(predicate)

    fun ClassDef.firstMethodDeclaratively(
        predicate: DeclarativePredicate<Method>
    ) = requireNotNull(firstMethodDeclarativelyOrNull(predicate))
}

object IterableClassDefClassDefMatching {
    fun Iterable<ClassDef>.firstClassDefOrNull(
        predicate: ClassDefPredicate = { true }
    ) = withPredicateContext { firstOrNull { it.predicate() } }

    fun Iterable<ClassDef>.firstClassDef(
        predicate: ClassDefPredicate = { true }
    ) = requireNotNull(firstClassDefOrNull(predicate))

    fun Iterable<ClassDef>.firstClassDefOrNull(
        type: String? = null,
        predicate: ClassDefPredicate = { true }
    ) = if (type == null) firstClassDefOrNull(predicate)
    else withPredicateContext { firstOrNull { it.type == type && it.predicate() } }

    fun Iterable<ClassDef>.firstClassDef(
        type: String? = null,
        predicate: ClassDefPredicate = { true }
    ) = requireNotNull(firstClassDefOrNull(type, predicate))

    context(context: BytecodePatchContext)
    fun Iterable<ClassDef>.firstMutableClassDefOrNull(
        type: String? = null,
        predicate: ClassDefPredicate = { true }
    ) = firstClassDefOrNull(type, predicate)?.let { context.classDefs.getOrReplaceMutable(it) }

    context(_: BytecodePatchContext)
    fun Iterable<ClassDef>.firstMutableClassDef(
        type: String? = null,
        predicate: ClassDefPredicate = { true }
    ) = requireNotNull(firstMutableClassDefOrNull(type, predicate))

    fun Iterable<ClassDef>.firstClassDefDeclarativelyOrNull(
        type: String? = null,
        predicate: DeclarativePredicate<ClassDef>
    ) = firstClassDefOrNull(type) { rememberDeclarativePredicate(predicate) }

    fun Iterable<ClassDef>.firstClassDefDeclaratively(
        type: String? = null,
        predicate: DeclarativePredicate<ClassDef>
    ) = requireNotNull(firstClassDefDeclarativelyOrNull(type, predicate))

    context(_: BytecodePatchContext)
    fun Iterable<ClassDef>.firstMutableClassDefDeclarativelyOrNull(
        type: String? = null,
        predicate: DeclarativePredicate<ClassDef>
    ) = firstMutableClassDefOrNull(type) { rememberDeclarativePredicate(predicate) }

    context(_: BytecodePatchContext)
    fun Iterable<ClassDef>.firstMutableClassDefDeclaratively(
        type: String? = null,
        predicate: DeclarativePredicate<ClassDef>
    ) = requireNotNull(firstMutableClassDefDeclarativelyOrNull(type, predicate))
}

object BytecodePatchContextMethodMatching {
    fun BytecodePatchContext.firstMethodOrNull(
        methodReference: MethodReference
    ) = firstClassDefOrNull(methodReference.definingClass)?.methods?.firstMethodOrNull(methodReference)

    fun BytecodePatchContext.firstMethod(
        method: MethodReference
    ) = requireNotNull(firstMethodOrNull(method))

    fun BytecodePatchContext.firstMutableMethodOrNull(
        methodReference: MethodReference
    ): MutableMethod? = firstMutableClassDefOrNull(methodReference.definingClass)?.methods
        ?.first { MethodUtil.methodSignaturesMatch(methodReference, it) }

    fun BytecodePatchContext.firstMutableMethod(
        method: MethodReference
    ) = requireNotNull(firstMutableMethodOrNull(method))

    fun BytecodePatchContext.firstMethodOrNull(
        vararg strings: String,
        predicate: MethodPredicate = { true },
    ): Method? = withPredicateContext {
        if (strings.isEmpty()) return classDefs.firstMethodOrNull(predicate)

        val methodsWithStrings = strings.mapNotNull { classDefs.methodsByString[it] }
        if (methodsWithStrings.size != strings.size) return null

        return methodsWithStrings.minBy { it.size }.firstOrNull { method ->
            val containsAllOtherStrings = methodsWithStrings.all { method in it }
            containsAllOtherStrings && method.predicate()
        }
    }

    fun BytecodePatchContext.firstMethod(
        vararg strings: String,
        predicate: MethodPredicate = { true },
    ) = requireNotNull(firstMethodOrNull(strings = strings, predicate))

    fun BytecodePatchContext.firstMutableMethodOrNull(
        vararg strings: String,
        predicate: MethodPredicate = { true },
    ) = firstMethodOrNull(strings = strings, predicate)?.let { method ->
        firstMutableMethodOrNull(method)
    }

    fun BytecodePatchContext.firstMutableMethod(
        vararg strings: String,
        predicate: MethodPredicate = { true }
    ) = requireNotNull(firstMutableMethodOrNull(strings = strings, predicate))

    fun gettingFirstMethodOrNull(
        method: MethodReference
    ) = cachedReadOnlyProperty { firstMethodOrNull(method) }

    fun gettingFirstMethod(
        method: MethodReference
    ) = cachedReadOnlyProperty { firstMethod(method) }

    fun gettingFirstMutableMethodOrNull(
        method: MethodReference
    ) = cachedReadOnlyProperty { firstMutableMethodOrNull(method) }

    fun gettingFirstMutableMethod(
        method: MethodReference
    ) = cachedReadOnlyProperty { firstMutableMethod(method) }

    fun gettingFirstMethodOrNull(
        vararg strings: String,
        predicate: MethodPredicate = { true },
    ) = cachedReadOnlyProperty { firstMethodOrNull(strings = strings, predicate) }

    fun gettingFirstMethod(
        vararg strings: String,
        predicate: MethodPredicate = { true },
    ) = cachedReadOnlyProperty { firstMethod(strings = strings, predicate) }

    fun gettingFirstMutableMethodOrNull(
        vararg strings: String,
        predicate: MethodPredicate = { true },
    ) = cachedReadOnlyProperty { firstMutableMethodOrNull(strings = strings, predicate) }

    fun gettingFirstMutableMethod(
        vararg strings: String,
        predicate: MethodPredicate = { true },
    ) = cachedReadOnlyProperty { firstMutableMethod(strings = strings, predicate) }

    fun BytecodePatchContext.firstMethodDeclarativelyOrNull(
        vararg strings: String,
        predicate: DeclarativePredicate<Method> = { }
    ) = firstMethodOrNull(strings = strings) { rememberDeclarativePredicate(predicate) }

    fun BytecodePatchContext.firstMethodDeclaratively(
        vararg strings: String,
        predicate: DeclarativePredicate<Method> = { }
    ) = requireNotNull(firstMethodDeclarativelyOrNull(strings = strings, predicate))

    fun BytecodePatchContext.firstMutableMethodDeclarativelyOrNull(
        vararg strings: String,
        predicate: DeclarativePredicate<Method> = { }
    ) = firstMutableMethodOrNull(strings = strings) { rememberDeclarativePredicate(predicate) }

    fun BytecodePatchContext.firstMutableMethodDeclaratively(
        vararg strings: String,
        predicate: DeclarativePredicate<Method> = { }
    ) = requireNotNull(firstMutableMethodDeclarativelyOrNull(strings = strings, predicate))

    fun gettingFirstMethodDeclarativelyOrNull(
        vararg strings: String,
        predicate: DeclarativePredicate<Method> = { }
    ) = gettingFirstMethodOrNull(strings = strings) { rememberDeclarativePredicate(predicate) }

    fun gettingFirstMethodDeclaratively(
        vararg strings: String,
        predicate: DeclarativePredicate<Method> = { }
    ) = gettingFirstMethod(strings = strings) { rememberDeclarativePredicate(predicate) }

    fun gettingFirstMutableMethodDeclarativelyOrNull(
        vararg strings: String,
        predicate: DeclarativePredicate<Method> = { }
    ) = gettingFirstMutableMethodOrNull(strings = strings) { rememberDeclarativePredicate(predicate) }

    fun gettingFirstMutableMethodDeclaratively(
        vararg strings: String,
        predicate: DeclarativePredicate<Method> = { }
    ) = gettingFirstMutableMethod(strings = strings) { rememberDeclarativePredicate(predicate) }
}

object BytecodePatchContextClassDefMatching {
    fun BytecodePatchContext.firstClassDefOrNull(
        type: String? = null,
        predicate: ClassDefPredicate = { true }
    ) = withPredicateContext {
        if (type == null) classDefs.firstClassDefOrNull(predicate)
        else classDefs[type]?.takeIf { it.predicate() }
    }

    fun BytecodePatchContext.firstClassDef(
        type: String? = null,
        predicate: ClassDefPredicate = { true }
    ) = requireNotNull(firstClassDefOrNull(type, predicate))

    fun BytecodePatchContext.firstMutableClassDefOrNull(
        type: String? = null,
        predicate: ClassDefPredicate = { true }
    ) = firstClassDefOrNull(type, predicate)?.let { classDefs.getOrReplaceMutable(it) }

    fun BytecodePatchContext.firstMutableClassDef(
        type: String? = null,
        predicate: ClassDefPredicate = { true }
    ) = requireNotNull(firstMutableClassDefOrNull(type, predicate))

    fun gettingFirstClassDefOrNull(
        type: String? = null,
        predicate: ClassDefPredicate = { true }
    ) = cachedReadOnlyProperty { firstClassDefOrNull(type, predicate) }

    fun gettingFirstClassDef(
        type: String? = null,
        predicate: ClassDefPredicate = { true }
    ) = requireNotNull(gettingFirstClassDefOrNull(type, predicate))

    fun gettingFirstMutableClassDefOrNull(
        type: String? = null,
        predicate: ClassDefPredicate = { true }
    ) = cachedReadOnlyProperty { firstMutableClassDefOrNull(type, predicate) }

    fun gettingFirstMutableClassDef(
        type: String? = null,
        predicate: ClassDefPredicate = { true }
    ) = requireNotNull(gettingFirstMutableClassDefOrNull(type, predicate))

    fun BytecodePatchContext.firstClassDefDeclarativelyOrNull(
        type: String? = null,
        predicate: DeclarativePredicate<ClassDef> = { }
    ) = firstClassDefOrNull(type) { rememberDeclarativePredicate(predicate) }

    fun BytecodePatchContext.firstClassDefDeclaratively(
        type: String? = null,
        predicate: DeclarativePredicate<ClassDef> = { }
    ) = requireNotNull(firstClassDefDeclarativelyOrNull(type, predicate))

    fun BytecodePatchContext.firstMutableClassDefDeclarativelyOrNull(
        type: String? = null,
        predicate: DeclarativePredicate<ClassDef> = { }
    ) = firstMutableClassDefOrNull(type) { rememberDeclarativePredicate(predicate) }

    fun BytecodePatchContext.firstMutableClassDefDeclaratively(
        type: String? = null,
        predicate: DeclarativePredicate<ClassDef> = { }
    ) = requireNotNull(firstMutableClassDefDeclarativelyOrNull(type, predicate))

    fun gettingFirstClassDefDeclarativelyOrNull(
        type: String? = null,
        predicate: DeclarativePredicate<ClassDef> = { }
    ) = cachedReadOnlyProperty { firstClassDefDeclarativelyOrNull(type, predicate) }

    fun gettingFirstClassDefDeclaratively(
        type: String? = null,
        predicate: DeclarativePredicate<ClassDef> = { }
    ) = requireNotNull(gettingFirstClassDefDeclarativelyOrNull(type, predicate))

    fun gettingFirstMutableClassDefDeclarativelyOrNull(
        type: String? = null,
        predicate: DeclarativePredicate<ClassDef> = { }
    ) = cachedReadOnlyProperty { firstMutableClassDefDeclarativelyOrNull(type, predicate) }

    fun gettingFirstMutableClassDefDeclaratively(
        type: String? = null,
        predicate: DeclarativePredicate<ClassDef> = { }
    ) = requireNotNull(gettingFirstMutableClassDefDeclarativelyOrNull(type, predicate))
}

class PredicateContext internal constructor() : MutableMap<Any, Any?> by mutableMapOf()

private inline fun <T> withPredicateContext(block: PredicateContext.() -> T) = PredicateContext().block()

// region Matcher

// region IndexedMatcher

fun <T> indexedMatcher() = IndexedMatcher<T>()

fun <T> indexedMatcher(build: IndexedMatcher<T>.() -> Unit) =
    IndexedMatcher<T>().apply(build)

fun <T> Iterable<T>.matchIndexed(build: IndexedMatcher<T>.() -> Unit) =
    indexedMatcher(build)(this)

context(_: PredicateContext)
fun <T> Iterable<T>.rememberMatchIndexed(key: Any, build: IndexedMatcher<T>.() -> Unit) =
    indexedMatcher<T>()(key, this, build)

fun <T> head(
    predicate: T.(lastMatchedIndex: Int, currentIndex: Int) -> Boolean
): T.(Int, Int) -> Boolean = { lastMatchedIndex, currentIndex ->
    currentIndex == 0 && predicate(lastMatchedIndex, currentIndex)
}

fun <T> head(predicate: T.() -> Boolean): T.(Int, Int) -> Boolean =
    head { _, _ -> predicate() }

context(matcher: IndexedMatcher<T>)
fun <T> after(
    range: IntRange = 1..1,
    predicate: T.(lastMatchedIndex: Int, currentIndex: Int) -> Boolean
): T.(Int, Int) -> Boolean = predicate@{ lastMatchedIndex, currentIndex ->
    val distance = currentIndex - lastMatchedIndex

    matcher.nextIndex = when {
        distance < range.first -> lastMatchedIndex + range.first
        distance > range.last -> -1
        else -> return@predicate predicate(lastMatchedIndex, currentIndex)
    }

    false
}

context(_: IndexedMatcher<T>)
fun <T> after(range: IntRange = 1..1, predicate: T.() -> Boolean) =
    after(range) { _, _ -> predicate() }

context(matcher: IndexedMatcher<T>)
operator fun <T> (T.(Int, Int) -> Boolean).unaryPlus() = matcher.add(this)

class IndexedMatcher<T> : Matcher<T, T.(lastMatchedIndex: Int, currentIndex: Int) -> Boolean>() {
    val indices: List<Int>
        field = mutableListOf()

    private var lastMatchedIndex = -1
    private var currentIndex = -1
    var nextIndex: Int? = null

    override fun invoke(haystack: Iterable<T>): Boolean {
        // Normalize to list
        val hay = haystack as? List<T> ?: haystack.toList()

        indices.clear()
        this@IndexedMatcher.lastMatchedIndex = -1
        currentIndex = -1

        data class Frame(
            val patternIndex: Int,
            val lastMatchedIndex: Int,
            val previousFrame: Frame?,
            var nextHayIndex: Int,
            val matchedIndex: Int
        )

        val stack = ArrayDeque<Frame>()
        stack.add(
            Frame(
                patternIndex = 0,
                lastMatchedIndex = -1,
                previousFrame = null,
                nextHayIndex = 0,
                matchedIndex = -1
            )
        )

        while (stack.isNotEmpty()) {
            val frame = stack.last()

            if (frame.nextHayIndex >= hay.size || nextIndex == -1) {
                stack.removeLast()
                nextIndex = null
                continue
            }

            val i = frame.nextHayIndex
            currentIndex = i
            lastMatchedIndex = frame.lastMatchedIndex
            nextIndex = null

            if (this[frame.patternIndex](hay[i], lastMatchedIndex, currentIndex)) {
                Frame(
                    patternIndex = frame.patternIndex + 1,
                    lastMatchedIndex = i,
                    previousFrame = frame,
                    nextHayIndex = i + 1,
                    matchedIndex = i
                ).also {
                    if (it.patternIndex == size) {
                        indices += buildList(size) {
                            var frame: Frame? = it
                            while (frame != null && frame.matchedIndex != -1) {
                                add(frame.matchedIndex)
                                frame = frame.previousFrame
                            }
                        }.asReversed()

                        return true
                    }
                }.let(stack::add)
            }

            frame.nextHayIndex = when (val nextIndex = nextIndex) {
                null -> frame.nextHayIndex + 1
                -1 -> 0 // Frame will be removed next loop.
                else -> nextIndex
            }
        }

        return false
    }
}

// endregion

context(context: PredicateContext)
inline operator fun <T, U, reified M : Matcher<T, U>> M.invoke(
    key: Any,
    iterable: Iterable<T>,
    builder: M.() -> Unit
) = context.remember(key) { apply(builder) }(iterable)

context(_: PredicateContext)
inline operator fun <T, U, reified M : Matcher<T, U>> M.invoke(
    iterable: Iterable<T>,
    builder: M.() -> Unit
) = invoke(this@invoke.hashCode(), iterable, builder)

abstract class Matcher<T, U> : MutableList<U> by mutableListOf() {
    var matchIndex = -1
        protected set

    abstract operator fun invoke(haystack: Iterable<T>): Boolean
}

// endregion Matcher

context(list: MutablePredicateList<T>)
fun <T> allOf(block: MutablePredicateList<T>.() -> Unit) {
    val child = MutablePredicateList<T>().apply(block)
    list.add { child.all { it() } }
}

context(list: MutablePredicateList<T>)
fun <T> anyOf(block: MutablePredicateList<T>.() -> Unit) {
    val child = MutablePredicateList<T>().apply(block)
    list.add { child.any { it() } }
}

context(list: MutablePredicateList<T>)
fun <T> predicate(block: T.() -> Boolean) {
    list.add(block)
}

context(list: MutablePredicateList<T>)
fun <T> all(target: T): Boolean = list.all { target.it() }

context(list: MutablePredicateList<T>)
fun <T> any(target: T): Boolean = list.any { target.it() }

fun MutablePredicateList<Method>.accessFlags(vararg flags: AccessFlags) =
    predicate { accessFlags(flags = flags) }

fun MutablePredicateList<Method>.returnType(
    returnType: String,
    compare: String.(String) -> Boolean = String::startsWith
) = predicate { this.returnType.compare(returnType) }

fun MutablePredicateList<Method>.name(
    name: String,
    compare: String.(String) -> Boolean = String::equals
) = predicate { this.name.compare(name) }

fun MutablePredicateList<Method>.definingClass(
    definingClass: String,
    compare: String.(String) -> Boolean = String::equals
) = predicate { this.definingClass.compare(definingClass) }

fun MutablePredicateList<Method>.parameterTypes(vararg parameterTypePrefixes: String) = predicate {
    parameterTypes.size == parameterTypePrefixes.size && parameterTypes.zip(parameterTypePrefixes)
        .all { (a, b) -> a.startsWith(b) }
}

fun MutablePredicateList<Method>.instructions(
    build: IndexedMatcher<Instruction>.() -> Unit
) {
    val match = indexedMatcher<Instruction>()
    predicate { implementation { match(instructions) } }
}

fun MutablePredicateList<Method>.instructions(
    vararg predicates: Instruction.(currentIndex: Int, lastMatchedIndex: Int) -> Boolean
) = instructions {
    predicates.forEach { +it }
}

context(matcher: IndexedMatcher<Instruction>)
fun MutablePredicateList<Method>.instructions(
    build: IndexedMatcher<Instruction>.() -> Unit
) {
    matcher.build()
    predicate { implementation { matcher(instructions) } }
}

context(matcher: IndexedMatcher<Instruction>)
fun MutablePredicateList<Method>.instructions(
    vararg predicates: Instruction.(currentIndex: Int, lastMatchedIndex: Int) -> Boolean
) = instructions {
    predicates.forEach { +it }
}

fun MutablePredicateList<Method>.custom(block: Method.() -> Boolean) {
    predicate { block() }
}

context(_: IndexedMatcher<Instruction>)
inline fun <reified T : Instruction> `is`(
    crossinline predicate: T.() -> Boolean = { true }
): Instruction.(Int, Int) -> Boolean = { _, _ -> (this as? T)?.predicate() == true }

fun instruction(predicate: Instruction.() -> Boolean): Instruction.(Int, Int) -> Boolean = { _, _ -> predicate() }

fun registers(predicate: IntArray.() -> Boolean = { true }): Instruction.(Int, Int) -> Boolean = { _, _ ->
    when (this) {
        is RegisterRangeInstruction ->
            IntArray(registerCount) { startRegister + it }.predicate()

        is FiveRegisterInstruction ->
            intArrayOf(registerC, registerD, registerE, registerF, registerG).predicate()

        is ThreeRegisterInstruction ->
            intArrayOf(registerA, registerB, registerC).predicate()

        is TwoRegisterInstruction ->
            intArrayOf(registerA, registerB).predicate()

        is OneRegisterInstruction ->
            intArrayOf(registerA).predicate()

        else -> false
    }
}

fun registers(
    vararg registers: Int,
    compare: IntArray.(registers: IntArray) -> Boolean = { registers ->
        this.size >= registers.size && registers.indices.all { this[it] == registers[it] }
    }
) = registers({ compare(registers) })

fun literal(predicate: Long.() -> Boolean = { true }): Instruction.(Int, Int) -> Boolean =
    { _, _ -> wideLiteral?.predicate() == true }

fun literal(literal: Long, compare: Long.(Long) -> Boolean = Long::equals) =
    literal { compare(literal) }

fun reference(predicate: String.() -> Boolean = { true }): Instruction.(Int, Int) -> Boolean =
    predicate@{ _, _ -> this.reference?.toString()?.predicate() == true }

fun reference(reference: String, compare: String.(String) -> Boolean = String::equals) =
    reference { compare(reference) }

fun field(predicate: String.() -> Boolean = { true }): Instruction.(Int, Int) -> Boolean = { _, _ ->
    fieldReference?.name?.predicate() == true
}

fun field(name: String, compare: String.(String) -> Boolean = String::equals) =
    field { compare(name) }

fun type(predicate: String.() -> Boolean = { true }): Instruction.(Int, Int) -> Boolean =
    { _, _ -> type?.predicate() == true }

fun type(type: String, compare: String.(String) -> Boolean = String::equals) =
    type { compare(type) }

fun method(predicate: String.() -> Boolean = { true }): Instruction.(Int, Int) -> Boolean = { _, _ ->
    methodReference?.name?.predicate() == true
}

fun method(name: String, compare: String.(String) -> Boolean = String::equals) =
    method { compare(name) }

fun string(compare: String.() -> Boolean = { true }): Instruction.(Int, Int) -> Boolean = predicate@{ _, _ ->
    this@predicate.string?.compare() == true
}

context(stringsList: MutableList<String>)
fun string(
    string: String,
    compare: String.(String) -> Boolean = String::equals
): Instruction.(Int, Int) -> Boolean {
    if (compare == String::equals) stringsList += string

    return string { compare(string) }
}

fun string(string: String, compare: String.(String) -> Boolean = String::equals) = string { compare(string) }

context(stringsList: MutableList<String>)
operator fun String.invoke(compare: String.(String) -> Boolean = String::equals): Instruction.(Int, Int) -> Boolean {
    if (compare == String::equals) stringsList += this

    return { _, _ -> string?.compare(this@invoke) == true }
}

operator fun Opcode.invoke(): Instruction.(currentIndex: Int, lastMatchedIndex: Int) -> Boolean =
    { _, _ -> opcode == this@invoke }

fun anyOf(
    vararg predicates: Instruction.(currentIndex: Int, lastMatchedIndex: Int) -> Boolean
): Instruction.(Int, Int) -> Boolean = { currentIndex, lastMatchedIndex ->
    predicates.any { predicate -> predicate(currentIndex, lastMatchedIndex) }
}

fun allOf(
    vararg predicates: Instruction.(currentIndex: Int, lastMatchedIndex: Int) -> Boolean
): Instruction.(Int, Int) -> Boolean = { currentIndex, lastMatchedIndex ->
    predicates.all { predicate -> predicate(currentIndex, lastMatchedIndex) }
}

fun noneOf(
    vararg predicates: Instruction.(currentIndex: Int, lastMatchedIndex: Int) -> Boolean
): Instruction.(Int, Int) -> Boolean = { currentIndex, lastMatchedIndex ->
    predicates.none { predicate -> predicate(currentIndex, lastMatchedIndex) }
}

private typealias BuildDeclarativePredicate<Method> = context(
PredicateContext,
IndexedMatcher<Instruction>,
MutableList<String>
) MutablePredicateList<Method>.() -> Unit

fun firstMethodComposite(
    vararg strings: String,
    build: BuildDeclarativePredicate<Method>
) = MatchBuilder(strings = strings, build)

val a = firstMethodComposite {
    name("exampleMethod")
    definingClass("Lcom/example/MyClass;")
    returnType("V")
    instructions(
        head(Opcode.RETURN_VOID()),
        after(1..5, Opcode.INVOKE_VIRTUAL())
    )
}

class MatchBuilder private constructor(
    val strings: MutableList<String>,
    indexedMatcher: IndexedMatcher<Instruction>,
    build: BuildDeclarativePredicate<Method>,
) {

    internal constructor(vararg strings: String, build: BuildDeclarativePredicate<Method>) :
            this(strings = mutableListOf(elements = strings), indexedMatcher(), build)

    private val predicate: DeclarativePredicate<Method> = context(strings, indexedMatcher) { { build() } }

    private val indices = indexedMatcher.indices

    private val BytecodePatchContext.cachedImmutableMethodOrNull
            by gettingFirstMethodDeclarativelyOrNull(strings = strings.toTypedArray(), predicate)

    private val BytecodePatchContext.cachedImmutableClassDefOrNull by cachedReadOnlyProperty {
        val type = cachedImmutableMethodOrNull?.definingClass ?: return@cachedReadOnlyProperty null
        firstClassDefOrNull(type)
    }

    context(context: BytecodePatchContext)
    val immutableMethodOrNull get() = context.cachedImmutableMethodOrNull

    context(_: BytecodePatchContext)
    val immutableMethod get() = requireNotNull(immutableMethodOrNull)

    context(context: BytecodePatchContext)
    val immutableClassDefOrNull get() = context.cachedImmutableClassDefOrNull

    context(context: BytecodePatchContext)
    val immutableClassDef get() = requireNotNull(immutableClassDefOrNull)

    val BytecodePatchContext.cachedMethodOrNull by cachedReadOnlyProperty {
        firstMutableMethodOrNull(immutableMethodOrNull ?: return@cachedReadOnlyProperty null)
    }

    private val BytecodePatchContext.cachedClassDefOrNull by cachedReadOnlyProperty {
        val type = immutableMethodOrNull?.definingClass ?: return@cachedReadOnlyProperty null
        firstMutableClassDefOrNull(type)
    }

    context(context: BytecodePatchContext)
    val methodOrNull get() = context.cachedMethodOrNull

    context(_: BytecodePatchContext)
    val method get() = requireNotNull(methodOrNull)

    context(context: BytecodePatchContext)
    val classDefOrNull get() = context.cachedClassDefOrNull

    context(_: BytecodePatchContext)
    val classDef get() = requireNotNull(classDefOrNull)

    context(context: BytecodePatchContext)
    fun match(classDef: ClassDef) = Match(
        context,
        classDef.firstMethodDeclarativelyOrNull { predicate() },
        indices.toList()
    )
}

class Match(
    val context: BytecodePatchContext,
    val immutableMethodOrNull: Method?,
    val indices: List<Int>
) {
    val immutableMethod by lazy { requireNotNull(immutableMethodOrNull) }

    val methodOrNull by lazy {
        context.firstMutableMethodOrNull(immutableMethodOrNull ?: return@lazy null)
    }

    val method by lazy { requireNotNull(methodOrNull) }

    val immutableClassDefOrNull by lazy { context(context) { immutableMethodOrNull?.immutableClassDefOrNull } }

    val immutableClassDef by lazy { requireNotNull(context(context) { immutableMethod.immutableClassDef }) }

    val classDefOrNull by lazy {
        context.firstMutableClassDefOrNull(immutableMethodOrNull?.definingClass ?: return@lazy null)
    }

    val classDef by lazy { requireNotNull(classDefOrNull) }
}

context(context: BytecodePatchContext)
val Method.immutableClassDefOrNull get() = context.classDefs[definingClass]

context(_: BytecodePatchContext)
val Method.immutableClassDef get() = requireNotNull(immutableClassDefOrNull)

context(context: BytecodePatchContext)
val Method.classDefOrNull get() = context.firstMutableClassDefOrNull(definingClass)

context(_: BytecodePatchContext)
val Method.classDef get() = requireNotNull(classDefOrNull)
