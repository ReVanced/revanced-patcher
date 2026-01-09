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
import com.android.tools.smali.dexlib2.iface.reference.FieldReference
import com.android.tools.smali.dexlib2.iface.reference.MethodReference
import com.android.tools.smali.dexlib2.iface.reference.Reference
import com.android.tools.smali.dexlib2.mutable.MutableMethod
import com.android.tools.smali.dexlib2.util.MethodUtil
import kotlin.properties.ReadOnlyProperty
import kotlin.reflect.KProperty

typealias Predicate<T> = T.() -> Boolean
typealias Function<T> = T.() -> Unit

fun Iterable<ClassDef>.anyClassDef(predicate: Predicate<ClassDef>) = any(predicate)

fun ClassDef.anyMethod(predicate: Predicate<Method>) = methods.any(predicate)

fun ClassDef.anyDirectMethod(predicate: Predicate<Method>) = directMethods.any(predicate)

fun ClassDef.anyVirtualMethod(predicate: Predicate<Method>) = virtualMethods.any(predicate)

fun ClassDef.anyField(predicate: Predicate<Field>) = fields.any(predicate)

fun ClassDef.anyInstanceField(predicate: Predicate<Field>) = instanceFields.any(predicate)

fun ClassDef.anyStaticField(predicate: Predicate<Field>) = staticFields.any(predicate)

fun ClassDef.anyInterface(predicate: Predicate<String>) = interfaces.any(predicate)

fun ClassDef.anyAnnotation(predicate: Predicate<Annotation>) = annotations.any(predicate)

fun Method.implementation(predicate: Predicate<MethodImplementation>) = implementation?.predicate() ?: false

fun Method.anyParameter(predicate: Predicate<MethodParameter>) = parameters.any(predicate)

fun Method.anyParameterType(predicate: Predicate<CharSequence>) = parameterTypes.any(predicate)

fun Method.anyAnnotation(predicate: Predicate<Annotation>) = annotations.any(predicate)

fun Method.anyHiddenApiRestriction(predicate: Predicate<HiddenApiRestriction>) = hiddenApiRestrictions.any(predicate)

fun MethodImplementation.anyInstruction(predicate: Predicate<Instruction>) = instructions.any(predicate)

fun MethodImplementation.anyTryBlock(predicate: Predicate<TryBlock<out ExceptionHandler>>) = tryBlocks.any(predicate)

fun MethodImplementation.anyDebugItem(predicate: Predicate<Any>) = debugItems.any(predicate)

fun Iterable<Instruction>.anyInstruction(predicate: Predicate<Instruction>) = any(predicate)

typealias ClassDefPredicate = context(PredicateContext) ClassDef.() -> Boolean
typealias MethodPredicate = context(PredicateContext) Method.() -> Boolean
typealias BytecodePatchContextMethodPredicate = context(BytecodePatchContext, PredicateContext) Method.() -> Boolean
typealias BytecodePatchContextClassDefPredicate = context(BytecodePatchContext, PredicateContext) ClassDef.() -> Boolean

inline fun <reified V> PredicateContext.remember(key: Any, defaultValue: () -> V) = if (key in this) get(key) as V
else defaultValue().also { put(key, it) }

private fun <T> cachedReadOnlyProperty(block: BytecodePatchContext.(KProperty<*>) -> T) =
    object : ReadOnlyProperty<BytecodePatchContext, T> {
        private val cache = HashMap<BytecodePatchContext, T>(1)

        override fun getValue(thisRef: BytecodePatchContext, property: KProperty<*>) =
            if (thisRef in cache) cache.getValue(thisRef)
            else cache.getOrPut(thisRef) { thisRef.block(property) }
    }

class MutablePredicateList<T> internal constructor() : MutableList<Predicate<T>> by mutableListOf()

typealias DeclarativePredicate<T> = context(PredicateContext) MutablePredicateList<T>.() -> Unit
typealias BytecodePatchContextDeclarativePredicate<T> = context(BytecodePatchContext, PredicateContext) MutablePredicateList<T>.() -> Unit

fun <T> T.declarativePredicate(build: Function<MutablePredicateList<T>>) =
    context(MutablePredicateList<T>().apply(build)) {
        all(this)
    }

context(context: PredicateContext)
fun <T> T.rememberDeclarativePredicate(key: Any, block: Function<MutablePredicateList<T>>) =
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
        vararg strings: String, predicate: DeclarativePredicate<Method>
    ) = firstMethodOrNull(strings = strings) { rememberDeclarativePredicate(predicate) }

    context(context: BytecodePatchContext)
    fun Iterable<ClassDef>.firstMethodDeclaratively(
        vararg strings: String, predicate: DeclarativePredicate<Method>
    ) = requireNotNull(firstMethodDeclarativelyOrNull(strings = strings, predicate))

    context(context: BytecodePatchContext)
    fun Iterable<ClassDef>.firstMutableMethodDeclarativelyOrNull(
        vararg strings: String, predicate: DeclarativePredicate<Method>
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
        type: String? = null, predicate: ClassDefPredicate = { true }
    ) = if (type == null) firstClassDefOrNull(predicate)
    else withPredicateContext { firstOrNull { it.type == type && it.predicate() } }

    fun Iterable<ClassDef>.firstClassDef(
        type: String? = null, predicate: ClassDefPredicate = { true }
    ) = requireNotNull(firstClassDefOrNull(type, predicate))

    context(context: BytecodePatchContext)
    fun Iterable<ClassDef>.firstMutableClassDefOrNull(
        type: String? = null, predicate: ClassDefPredicate = { true }
    ) = firstClassDefOrNull(type, predicate)?.let { context.classDefs.getOrReplaceMutable(it) }

    context(_: BytecodePatchContext)
    fun Iterable<ClassDef>.firstMutableClassDef(
        type: String? = null, predicate: ClassDefPredicate = { true }
    ) = requireNotNull(firstMutableClassDefOrNull(type, predicate))

    fun Iterable<ClassDef>.firstClassDefDeclarativelyOrNull(
        type: String? = null, predicate: DeclarativePredicate<ClassDef>
    ) = firstClassDefOrNull(type) { rememberDeclarativePredicate(predicate) }

    fun Iterable<ClassDef>.firstClassDefDeclaratively(
        type: String? = null, predicate: DeclarativePredicate<ClassDef>
    ) = requireNotNull(firstClassDefDeclarativelyOrNull(type, predicate))

    context(_: BytecodePatchContext)
    fun Iterable<ClassDef>.firstMutableClassDefDeclarativelyOrNull(
        type: String? = null, predicate: DeclarativePredicate<ClassDef>
    ) = firstMutableClassDefOrNull(type) { rememberDeclarativePredicate(predicate) }

    context(_: BytecodePatchContext)
    fun Iterable<ClassDef>.firstMutableClassDefDeclaratively(
        type: String? = null, predicate: DeclarativePredicate<ClassDef>
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
    ): MutableMethod? = firstMutableClassDefOrNull(methodReference.definingClass)?.methods?.first {
        MethodUtil.methodSignaturesMatch(
            methodReference,
            it
        )
    }

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
        vararg strings: String, predicate: MethodPredicate = { true }
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
        predicate: BytecodePatchContextMethodPredicate = { true },
    ) = cachedReadOnlyProperty { firstMethodOrNull(strings = strings) { predicate() } }

    fun gettingFirstMethod(
        vararg strings: String,
        predicate: BytecodePatchContextMethodPredicate = { true },
    ) = cachedReadOnlyProperty { firstMethod(strings = strings) { predicate() } }

    fun gettingFirstMutableMethodOrNull(
        vararg strings: String,
        predicate: BytecodePatchContextMethodPredicate = { true },
    ) = cachedReadOnlyProperty { firstMutableMethodOrNull(strings = strings) { predicate() } }

    fun gettingFirstMutableMethod(
        vararg strings: String,
        predicate: BytecodePatchContextMethodPredicate = { true },
    ) = cachedReadOnlyProperty { firstMutableMethod(strings = strings) { predicate() } }

    fun BytecodePatchContext.firstMethodDeclarativelyOrNull(
        vararg strings: String, predicate: DeclarativePredicate<Method> = { }
    ) = firstMethodOrNull(strings = strings) { rememberDeclarativePredicate(predicate) }

    fun BytecodePatchContext.firstMethodDeclaratively(
        vararg strings: String, predicate: DeclarativePredicate<Method> = { }
    ) = requireNotNull(firstMethodDeclarativelyOrNull(strings = strings, predicate))

    fun BytecodePatchContext.firstMutableMethodDeclarativelyOrNull(
        vararg strings: String, predicate: DeclarativePredicate<Method> = { }
    ) = firstMutableMethodOrNull(strings = strings) { rememberDeclarativePredicate(predicate) }

    fun BytecodePatchContext.firstMutableMethodDeclaratively(
        vararg strings: String, predicate: DeclarativePredicate<Method> = { }
    ) = requireNotNull(firstMutableMethodDeclarativelyOrNull(strings = strings, predicate))

    fun gettingFirstMethodDeclarativelyOrNull(
        vararg strings: String,
        predicate: BytecodePatchContextDeclarativePredicate<Method> = { }
    ) = gettingFirstMethodOrNull(strings = strings) { rememberDeclarativePredicate { predicate() } }

    fun gettingFirstMethodDeclaratively(
        vararg strings: String,
        predicate: BytecodePatchContextDeclarativePredicate<Method> = { }
    ) = gettingFirstMethod(strings = strings) { rememberDeclarativePredicate { predicate() } }

    fun gettingFirstMutableMethodDeclarativelyOrNull(
        vararg strings: String,
        predicate: BytecodePatchContextDeclarativePredicate<Method> = { }
    ) = gettingFirstMutableMethodOrNull(strings = strings) { rememberDeclarativePredicate { predicate() } }

    fun gettingFirstMutableMethodDeclaratively(
        vararg strings: String,
        predicate: BytecodePatchContextDeclarativePredicate<Method> = { }
    ) = gettingFirstMutableMethod(strings = strings) { rememberDeclarativePredicate { predicate() } }
}

object BytecodePatchContextClassDefMatching {
    fun BytecodePatchContext.firstClassDefOrNull(
        type: String? = null, predicate: ClassDefPredicate = { true }
    ) = withPredicateContext {
        if (type == null) classDefs.firstClassDefOrNull(predicate)
        else classDefs[type]?.takeIf { it.predicate() }
    }

    fun BytecodePatchContext.firstClassDef(
        type: String? = null, predicate: ClassDefPredicate = { true }
    ) = requireNotNull(firstClassDefOrNull(type, predicate))

    fun BytecodePatchContext.firstMutableClassDefOrNull(
        type: String? = null, predicate: ClassDefPredicate = { true }
    ) = firstClassDefOrNull(type, predicate)?.let { classDefs.getOrReplaceMutable(it) }

    fun BytecodePatchContext.firstMutableClassDef(
        type: String? = null, predicate: ClassDefPredicate = { true }
    ) = requireNotNull(firstMutableClassDefOrNull(type, predicate))

    fun gettingFirstClassDefOrNull(
        type: String? = null, predicate: BytecodePatchContextClassDefPredicate = { true }
    ) = cachedReadOnlyProperty { firstClassDefOrNull(type) { predicate() } }

    fun gettingFirstClassDef(
        type: String? = null, predicate: BytecodePatchContextClassDefPredicate = { true }
    ) = requireNotNull(gettingFirstClassDefOrNull(type) { predicate() })

    fun gettingFirstMutableClassDefOrNull(
        type: String? = null, predicate: BytecodePatchContextClassDefPredicate = { true }
    ) = cachedReadOnlyProperty { firstMutableClassDefOrNull(type) { predicate() } }

    fun gettingFirstMutableClassDef(
        type: String? = null, predicate: BytecodePatchContextClassDefPredicate = { true }
    ) = requireNotNull(gettingFirstMutableClassDefOrNull(type, predicate))

    fun BytecodePatchContext.firstClassDefDeclarativelyOrNull(
        type: String? = null, predicate: DeclarativePredicate<ClassDef> = { }
    ) = firstClassDefOrNull(type) { rememberDeclarativePredicate(predicate) }

    fun BytecodePatchContext.firstClassDefDeclaratively(
        type: String? = null, predicate: DeclarativePredicate<ClassDef> = { }
    ) = requireNotNull(firstClassDefDeclarativelyOrNull(type, predicate))

    fun BytecodePatchContext.firstMutableClassDefDeclarativelyOrNull(
        type: String? = null, predicate: DeclarativePredicate<ClassDef> = { }
    ) = firstMutableClassDefOrNull(type) { rememberDeclarativePredicate(predicate) }

    fun BytecodePatchContext.firstMutableClassDefDeclaratively(
        type: String? = null, predicate: DeclarativePredicate<ClassDef> = { }
    ) = requireNotNull(firstMutableClassDefDeclarativelyOrNull(type, predicate))

    fun gettingFirstClassDefDeclarativelyOrNull(
        type: String? = null, predicate: BytecodePatchContextDeclarativePredicate<ClassDef> = { }
    ) = cachedReadOnlyProperty { firstClassDefDeclarativelyOrNull(type) { predicate() } }

    fun gettingFirstClassDefDeclaratively(
        type: String? = null, predicate: BytecodePatchContextDeclarativePredicate<ClassDef> = { }
    ) = requireNotNull(gettingFirstClassDefDeclarativelyOrNull(type) { predicate() })

    fun gettingFirstMutableClassDefDeclarativelyOrNull(
        type: String? = null, predicate: BytecodePatchContextDeclarativePredicate<ClassDef> = { }
    ) = cachedReadOnlyProperty { firstMutableClassDefDeclarativelyOrNull(type) { predicate() } }

    fun gettingFirstMutableClassDefDeclaratively(
        type: String? = null, predicate: BytecodePatchContextDeclarativePredicate<ClassDef> = { }
    ) = requireNotNull(gettingFirstMutableClassDefDeclarativelyOrNull(type) { predicate() })
}

class PredicateContext internal constructor() : MutableMap<Any, Any?> by mutableMapOf()

private inline fun <T> withPredicateContext(block: PredicateContext.() -> T) = PredicateContext().block()

// region Matcher

// region IndexedMatcher

fun <T> indexedMatcher(vararg items: IndexedMatcherPredicate<T>) = IndexedMatcher<T>().apply {
    items.forEach { +it }
}

fun <T> indexedMatcher(build: Function<IndexedMatcher<T>>) = IndexedMatcher<T>().apply(build)

fun <T> Iterable<T>.matchIndexed(build: Function<IndexedMatcher<T>>) = indexedMatcher(build)(this)

context(_: PredicateContext)
fun <T> Iterable<T>.rememberMatchIndexed(key: Any, build: Function<IndexedMatcher<T>>) =
    indexedMatcher<T>()(key, this, build)

context(_: PredicateContext)
fun <T> Iterable<T>.matchIndexed(
    key: Any, vararg items: IndexedMatcherPredicate<T>
) = indexedMatcher<T>()(key, this) { items.forEach { +it } }

fun <T> at(
    index: Int = 0, predicate: IndexedMatcherPredicate<T>
): IndexedMatcherPredicate<T> = { lastMatchedIndex, currentIndex, setNextIndex ->
    currentIndex == index && predicate(lastMatchedIndex, currentIndex, setNextIndex)
}

fun <T> at(index: Int = 0, predicate: Predicate<T>) = at<T>(index) { _, _, _ -> predicate() }

fun <T> at(predicate: IndexedMatcherPredicate<T>): IndexedMatcherPredicate<T> =
    at(0) { lastMatchedIndex, currentIndex, setNextIndex -> predicate(lastMatchedIndex, currentIndex, setNextIndex) }

fun <T> at(predicate: Predicate<T>) = at<T> { _, _, _ -> predicate() }

fun <T> after(
    range: IntRange = 1..1, predicate: IndexedMatcherPredicate<T>
): IndexedMatcherPredicate<T> = predicate@{ lastMatchedIndex, currentIndex, setNextIndex ->
    val distance = currentIndex - lastMatchedIndex

    setNextIndex(
        when {
            distance < range.first -> lastMatchedIndex + range.first
            distance > range.last -> -1
            else -> return@predicate predicate(lastMatchedIndex, currentIndex, setNextIndex)
        }
    )

    false
}

fun <T> after(range: IntRange = 1..1, predicate: Predicate<T>) = after<T>(range) { _, _, _ -> predicate() }

fun <T> after(predicate: IndexedMatcherPredicate<T>) = after<T>(1..1) { lastMatchedIndex, currentIndex, setNextIndex ->
    predicate(lastMatchedIndex, currentIndex, setNextIndex)
}

fun <T> after(predicate: Predicate<T>) = after<T> { _, _, _ -> predicate() }


fun <T> anyOf(
    vararg predicates: IndexedMatcherPredicate<T>
): IndexedMatcherPredicate<T> = { currentIndex, lastMatchedIndex, setNextIndex ->
    predicates.any { predicate -> predicate(currentIndex, lastMatchedIndex, setNextIndex) }
}

fun <T> allOf(
    vararg predicates: IndexedMatcherPredicate<T>
): IndexedMatcherPredicate<T> = { currentIndex, lastMatchedIndex, setNextIndex ->
    predicates.all { predicate -> predicate(currentIndex, lastMatchedIndex, setNextIndex) }
}

fun <T> noneOf(
    vararg predicates: IndexedMatcherPredicate<T>
): IndexedMatcherPredicate<T> = { currentIndex, lastMatchedIndex, setNextIndex ->
    predicates.none { predicate -> predicate(currentIndex, lastMatchedIndex, setNextIndex) }
}

context(matcher: IndexedMatcher<T>)
operator fun <T> IndexedMatcherPredicate<T>.unaryPlus() = matcher.add(this)

typealias IndexedMatcherPredicate<T> = T.(lastMatchedIndex: Int, currentIndex: Int, setNextIndex: (Int?) -> Unit) -> Boolean

class IndexedMatcher<T> : Matcher<T, IndexedMatcherPredicate<T>>() {
    val indices: List<Int>
        field = mutableListOf()

    private var lastMatchedIndex = -1
    private var currentIndex = -1
    private var nextIndex: Int? = null

    override fun invoke(haystack: Iterable<T>): Boolean {
        // Ensure list for indexed access.
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
                patternIndex = 0, lastMatchedIndex = -1, previousFrame = null, nextHayIndex = 0, matchedIndex = -1
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

            if (this[frame.patternIndex](hay[i], lastMatchedIndex, currentIndex, this::nextIndex::set)) {
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
    key: Any, iterable: Iterable<T>, builder: Function<M>
) = context.remember(key) { apply(builder) }(iterable)

abstract class Matcher<T, U> : MutableList<U> by mutableListOf() {
    var matchIndex = -1
        protected set

    abstract operator fun invoke(haystack: Iterable<T>): Boolean
}

// endregion Matcher

context(list: MutablePredicateList<T>)
fun <T> allOf(block: Function<MutablePredicateList<T>>) {
    val child = MutablePredicateList<T>().apply(block)
    list.add { child.all { it() } }
}

context(list: MutablePredicateList<T>)
fun <T> anyOf(block: Function<MutablePredicateList<T>>) {
    val child = MutablePredicateList<T>().apply(block)
    list.add { child.any { it() } }
}

context(list: MutablePredicateList<T>)
fun <T> noneOf(block: Function<MutablePredicateList<T>>) {
    val child = MutablePredicateList<T>().apply(block)
    list.add { child.none { it() } }
}

context(list: MutablePredicateList<T>)
fun <T> predicate(block: Predicate<T>) {
    list.add(block)
}

context(list: MutablePredicateList<T>)
fun <T> all(target: T): Boolean = list.all { target.it() }

context(list: MutablePredicateList<T>)
fun <T> any(target: T): Boolean = list.any { target.it() }

fun MutablePredicateList<Method>.accessFlags(vararg flags: AccessFlags) = predicate { accessFlags(flags = flags) }

fun MutablePredicateList<Method>.returnType(
    predicate: Predicate<String>
) = predicate { this.returnType.predicate() }

fun MutablePredicateList<Method>.returnType(
    returnType: String, compare: String.(String) -> Boolean = String::startsWith
) = predicate { this.returnType.compare(returnType) }

fun MutablePredicateList<Method>.name(
    predicate: Predicate<String>
) = predicate { this.name.predicate() }

fun MutablePredicateList<Method>.name(
    name: String, compare: String.(String) -> Boolean = String::equals
) = predicate { this.name.compare(name) }

fun MutablePredicateList<Method>.definingClass(
    predicate: Predicate<String>
) = predicate { this.definingClass.predicate() }

fun MutablePredicateList<Method>.definingClass(
    definingClass: String, compare: String.(String) -> Boolean = String::equals
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
    vararg predicates: IndexedMatcherPredicate<Instruction>
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
    vararg predicates: IndexedMatcherPredicate<Instruction>
) = instructions { predicates.forEach { +it } }

fun MutablePredicateList<Method>.custom(block: Predicate<Method>) {
    predicate { block() }
}

inline fun <reified T : Instruction> `is`(
    crossinline predicate: Predicate<T> = { true }
): IndexedMatcherPredicate<Instruction> = { _, _, _ -> (this as? T)?.predicate() == true }

fun instruction(predicate: Predicate<Instruction> = { true }): IndexedMatcherPredicate<Instruction> =
    { _, _, _ -> predicate() }

fun registers(predicate: Predicate<IntArray> = { true }): IndexedMatcherPredicate<Instruction> = { _, _, _ ->
    when (this) {
        is RegisterRangeInstruction -> IntArray(registerCount) { startRegister + it }.predicate()

        is FiveRegisterInstruction -> intArrayOf(registerC, registerD, registerE, registerF, registerG).predicate()

        is ThreeRegisterInstruction -> intArrayOf(registerA, registerB, registerC).predicate()

        is TwoRegisterInstruction -> intArrayOf(registerA, registerB).predicate()

        is OneRegisterInstruction -> intArrayOf(registerA).predicate()

        else -> false
    }
}

fun registers(
    vararg registers: Int, compare: IntArray.(registers: IntArray) -> Boolean = { registers ->
        this.size >= registers.size && registers.indices.all { this[it] == registers[it] }
    }
) = registers({ compare(registers) })

fun literal(predicate: Predicate<Long> = { true }): IndexedMatcherPredicate<Instruction> =
    { _, _, _ -> wideLiteral?.predicate() == true }

fun literal(literal: Long, compare: Long.(Long) -> Boolean = Long::equals) = literal { compare(literal) }

operator fun Long.invoke(compare: Long.(Long) -> Boolean = Long::equals) = literal(this, compare)

inline fun <reified T : Reference> reference(
    crossinline predicate: Predicate<T> = { true }
): IndexedMatcherPredicate<Instruction> = { _, _, _ ->
    (reference as? T)?.predicate() == true
}

fun reference(
    reference: String, compare: String.(String) -> Boolean = String::equals
): IndexedMatcherPredicate<Instruction> = { _, _, _ -> this.reference?.toString()?.compare(reference) == true }

fun field(predicate: Predicate<FieldReference> = { true }): IndexedMatcherPredicate<Instruction> = { _, _, _ ->
    fieldReference?.predicate() == true
}

fun field(name: String, compare: String.(String) -> Boolean = String::equals) = field { this.name.compare(name) }

fun type(predicate: Predicate<String> = { true }): IndexedMatcherPredicate<Instruction> =
    { _, _, _ -> type?.predicate() == true }

fun type(type: String, compare: String.(type: String) -> Boolean = String::equals) = type { compare(type) }

fun method(predicate: Predicate<MethodReference> = { true }): IndexedMatcherPredicate<Instruction> = { _, _, _ ->
    methodReference?.predicate() == true
}

fun method(name: String, compare: String.(String) -> Boolean = String::equals) = method { this.name.compare(name) }

fun string(compare: Predicate<String> = { true }): IndexedMatcherPredicate<Instruction> = predicate@{ _, _, _ ->
    this@predicate.string?.compare() == true
}

context(stringsList: MutableList<String>)
fun string(
    string: String, compare: String.(String) -> Boolean = String::equals
): IndexedMatcherPredicate<Instruction> {
    if (compare == String::equals) stringsList += string

    return string { compare(string) }
}

fun string(string: String, compare: String.(String) -> Boolean = String::equals) = string { compare(string) }

operator fun String.invoke(compare: Predicate<String> = { true }): IndexedMatcherPredicate<Instruction> =
    { _, _, _ -> string?.compare() == true }

context(stringsList: MutableList<String>)
operator fun String.invoke(compare: String.(String) -> Boolean = String::equals): IndexedMatcherPredicate<Instruction> {
    if (compare == String::equals) stringsList += this

    return invoke(compare)
}

operator fun Opcode.invoke(): IndexedMatcherPredicate<Instruction> = { _, _, _ -> opcode == this@invoke }

typealias BuildCompositeDeclarativePredicate<Method> = context(BytecodePatchContext, PredicateContext, IndexedMatcher<Instruction>, MutableList<String>)
MutablePredicateList<Method>.() -> Unit

fun firstMethodComposite(
    vararg strings: String,
    build: BuildCompositeDeclarativePredicate<Method>
) = MatchBuilder(strings = strings, build)

class MatchBuilder private constructor(
    private val strings: MutableList<String>,
    indexedMatcher: IndexedMatcher<Instruction>,
    build: BuildCompositeDeclarativePredicate<Method>,
) {

    internal constructor(
        vararg strings: String,
        build: BuildCompositeDeclarativePredicate<Method>
    ) : this(strings = mutableListOf(elements = strings), indexedMatcher(), build)

    private val predicate: BytecodePatchContextDeclarativePredicate<Method> = {
        context(strings, indexedMatcher) { build() }
    }

    val indices = indexedMatcher.indices

    private val BytecodePatchContext.cachedImmutableMethodOrNull by gettingFirstMethodDeclarativelyOrNull(
        strings = strings.toTypedArray(),
        predicate
    )

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
    val context: BytecodePatchContext, val immutableMethodOrNull: Method?, val indices: List<Int>
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
