@file:Suppress("unused", "MemberVisibilityCanBePrivate", "CONTEXT_RECEIVERS_DEPRECATED")

package app.revanced.patcher

import app.revanced.patcher.Matcher.MatchContext
import app.revanced.patcher.dex.mutable.MutableMethod
import app.revanced.patcher.extensions.accessFlags
import app.revanced.patcher.patch.BytecodePatchContext
import com.android.tools.smali.dexlib2.AccessFlags
import com.android.tools.smali.dexlib2.HiddenApiRestriction
import com.android.tools.smali.dexlib2.iface.*
import com.android.tools.smali.dexlib2.iface.Annotation
import com.android.tools.smali.dexlib2.iface.instruction.Instruction
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

fun BytecodePatchContext.firstClassDefOrNull(predicate: context(MatchContext) ClassDef.() -> Boolean) =
    with(MatchContext()) { classDefs.firstOrNull { it.predicate() } }

fun BytecodePatchContext.firstClassDef(predicate: context(MatchContext) ClassDef.() -> Boolean) =
    requireNotNull(firstClassDefOrNull(predicate))

fun BytecodePatchContext.firstClassDefMutableOrNull(predicate: context(MatchContext) ClassDef.() -> Boolean) =
    firstClassDefOrNull(predicate)?.mutable()

fun BytecodePatchContext.firstClassDefMutable(predicate: context(MatchContext) ClassDef.() -> Boolean) =
    requireNotNull(firstClassDefMutableOrNull(predicate))

fun BytecodePatchContext.firstClassDefOrNull(
    type: String, predicate: (context(MatchContext) ClassDef.() -> Boolean)? = null
) = lookupMaps.classDefsByType[type]?.takeIf {
    predicate == null || with(MatchContext()) { it.predicate() }
}

fun BytecodePatchContext.firstClassDef(
    type: String, predicate: (context(MatchContext) ClassDef.() -> Boolean)? = null
) = requireNotNull(firstClassDefOrNull(type, predicate))

fun BytecodePatchContext.firstClassDefMutableOrNull(
    type: String, predicate: (context(MatchContext) ClassDef.() -> Boolean)? = null
) = firstClassDefOrNull(type, predicate)?.mutable()

fun BytecodePatchContext.firstClassDefMutable(
    type: String, predicate: (context(MatchContext) ClassDef.() -> Boolean)? = null
) = requireNotNull(firstClassDefMutableOrNull(type, predicate))

fun Iterable<ClassDef>.firstMethodOrNull(predicate: context(MatchContext) Method.() -> Boolean) =
    with(MatchContext()) {
        this@firstMethodOrNull.asSequence().flatMap { it.methods.asSequence() }.firstOrNull { it.predicate() }
    }

fun Iterable<ClassDef>.firstMethod(predicate: context(MatchContext) Method.() -> Boolean) =
    requireNotNull(firstMethodOrNull(predicate))

/** Can't compile due to JVM platform declaration clash
fun Iterable<Method>.firstMethodOrNull(predicate: context(MatchContext) Method.() -> Boolean) =
with(MatchContext()) { firstOrNull { it.predicate() } }

fun Iterable<Method>.firstMethod(predicate: context(MatchContext) Method.() -> Boolean) =
with(MatchContext()) { requireNotNull(firstMethodOrNull(predicate)) }
 **/
fun BytecodePatchContext.firstMethodOrNull(predicate: context(MatchContext) Method.() -> Boolean) =
    classDefs.firstMethodOrNull(predicate)

fun BytecodePatchContext.firstMethod(predicate: context(MatchContext) Method.() -> Boolean) =
    requireNotNull(firstMethodOrNull(predicate))

fun BytecodePatchContext.firstMethodMutableOrNull(predicate: context(MatchContext) Method.() -> Boolean) =
    classDefs.firstMethodOrNull(predicate)?.let { method ->
        lookupMaps.classDefsByType[method.definingClass]!!.mutable().methods.first {
            MethodUtil.methodSignaturesMatch(method, it)
        }
    }

fun BytecodePatchContext.firstMethodMutable(predicate: context(MatchContext) Method.() -> Boolean) =
    requireNotNull(firstMethodMutableOrNull(predicate))

fun BytecodePatchContext.firstMethodOrNull(
    vararg strings: String,
    predicate: context(MatchContext) Method.() -> Boolean = { true },
) = with(MatchContext()) {
    strings.mapNotNull { lookupMaps.methodsByStrings[it] }.minByOrNull { it.size }?.firstOrNull { it.predicate() }
}

fun BytecodePatchContext.firstMethod(
    vararg strings: String,
    predicate: context(MatchContext) Method.() -> Boolean = { true },
) = requireNotNull(firstMethodOrNull(*strings, predicate = predicate))

fun BytecodePatchContext.firstMethodMutableOrNull(
    vararg strings: String,
    predicate: context(MatchContext) Method.() -> Boolean = { true },
) = with(MatchContext()) {
    strings.mapNotNull { lookupMaps.methodsByStrings[it] }.minByOrNull { it.size }?.let { methods ->
        methods.firstOrNull { it.predicate() }?.let { method ->
            firstClassDefMutable(method.definingClass).methods.first {
                MethodUtil.methodSignaturesMatch(
                    method, it
                )
            }
        }
    }
}

fun BytecodePatchContext.firstMethodMutable(
    vararg strings: String, predicate: context(MatchContext) Method.() -> Boolean = { true }
) = requireNotNull(firstMethodMutableOrNull(*strings, predicate = predicate))

class CachedReadOnlyProperty<T> internal constructor(
    private val block: BytecodePatchContext.(KProperty<*>) -> T
) : ReadOnlyProperty<BytecodePatchContext, T> {
    private var value: T? = null
    private var cached = false

    override fun getValue(thisRef: BytecodePatchContext, property: KProperty<*>): T {
        if (!cached) {
            value = thisRef.block(property)
            cached = true
        }

        return value!!
    }
}

fun gettingFirstClassDefOrNull(predicate: context(MatchContext) ClassDef.() -> Boolean) =
    CachedReadOnlyProperty { firstClassDefOrNull(predicate) }

fun gettingFirstClassDef(predicate: context(MatchContext) ClassDef.() -> Boolean) =
    CachedReadOnlyProperty { firstClassDef(predicate) }

fun gettingFirstClassDefMutableOrNull(predicate: context(MatchContext) ClassDef.() -> Boolean) =
    CachedReadOnlyProperty { firstClassDefMutableOrNull(predicate) }

fun gettingFirstClassDefMutable(predicate: context(MatchContext) ClassDef.() -> Boolean) =
    CachedReadOnlyProperty { firstClassDefMutable(predicate) }

fun gettingFirstClassDefOrNull(
    type: String, predicate: (context(MatchContext) ClassDef.() -> Boolean)? = null
) = CachedReadOnlyProperty { firstClassDefOrNull(type, predicate) }

fun gettingFirstClassDef(
    type: String, predicate: (context(MatchContext) ClassDef.() -> Boolean)? = null
) = CachedReadOnlyProperty { firstClassDef(type, predicate) }

fun gettingFirstClassDefMutableOrNull(
    type: String, predicate: (context(MatchContext) ClassDef.() -> Boolean)? = null
) = CachedReadOnlyProperty { firstClassDefMutableOrNull(type, predicate) }

fun gettingFirstClassDefMutable(
    type: String, predicate: (context(MatchContext) ClassDef.() -> Boolean)? = null
) = CachedReadOnlyProperty { firstClassDefMutable(type, predicate) }

fun gettingFirstMethodOrNull(predicate: context(MatchContext) Method.() -> Boolean) =
    CachedReadOnlyProperty { firstMethodOrNull(predicate) }

fun gettingFirstMethod(predicate: context(MatchContext) Method.() -> Boolean) =
    CachedReadOnlyProperty { firstMethod(predicate) }

fun gettingFirstMethodMutableOrNull(predicate: context(MatchContext) Method.() -> Boolean) =
    CachedReadOnlyProperty { firstMethodMutableOrNull(predicate) }

fun gettingFirstMethodMutable(predicate: context(MatchContext) Method.() -> Boolean) =
    CachedReadOnlyProperty { firstMethodMutable(predicate) }

fun gettingFirstMethodOrNull(
    vararg strings: String,
    predicate: context(MatchContext) Method.() -> Boolean = { true },
) = CachedReadOnlyProperty { firstMethodOrNull(*strings, predicate = predicate) }

fun gettingFirstMethod(
    vararg strings: String,
    predicate: context(MatchContext) Method.() -> Boolean = { true },
) = CachedReadOnlyProperty { firstMethod(*strings, predicate = predicate) }

fun gettingFirstMethodMutableOrNull(
    vararg strings: String,
    predicate: context(MatchContext) Method.() -> Boolean = { true },
) = CachedReadOnlyProperty { firstMethodMutableOrNull(*strings, predicate = predicate) }

fun gettingFirstMethodMutable(
    vararg strings: String,
    predicate: context(MatchContext) Method.() -> Boolean = { true },
) = CachedReadOnlyProperty { firstMethodMutable(*strings, predicate = predicate) }

fun <T> indexedMatcher() = IndexedMatcher<T>()

fun <T> indexedMatcher(build: IndexedMatcher<T>.() -> Unit) =
    IndexedMatcher<T>().apply(build)

fun <T> Iterable<T>.matchIndexed(build: IndexedMatcher<T>.() -> Unit) =
    indexedMatcher(build)(this)

context(_: MatchContext)
fun <T> Iterable<T>.matchIndexed(key: Any, build: IndexedMatcher<T>.() -> Unit) =
    indexedMatcher<T>()(key, this, build)

context(_: MatchContext)
operator fun <T> IndexedMatcher<T>.invoke(key: Any, iterable: Iterable<T>, builder: IndexedMatcher<T>.() -> Unit) =
    remember(key) { apply(builder) }(iterable)

context(_: MatchContext)
operator fun <T> IndexedMatcher<T>.invoke(iterable: Iterable<T>, builder: IndexedMatcher<T>.() -> Unit) =
    invoke(this@invoke.hashCode(), iterable, builder)

abstract class Matcher<T, U> : MutableList<U> by mutableListOf() {
    var matchIndex = -1
        protected set

    abstract operator fun invoke(haystack: Iterable<T>): Boolean

    class MatchContext internal constructor() : MutableMap<Any, Any> by mutableMapOf()
}

context(context: MatchContext)
inline fun <reified V : Any> remember(key: Any, defaultValue: () -> V) =
    context[key] as? V ?: defaultValue().also { context[key] = it }

class IndexedMatcher<T>() : Matcher<T, T.(lastMatchedIndex: Int, currentIndex: Int) -> Boolean>() {
    private val _indices: MutableList<Int> = mutableListOf()
    val indices: List<Int> = _indices

    private var lastMatchedIndex = -1
    private var currentIndex = -1

    private var nextIndex: Int? = null

    override fun invoke(haystack: Iterable<T>): Boolean {
        // Normalize to list
        val hay = haystack as? List<T> ?: haystack.toList()

        _indices.clear()
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
                        _indices += buildList(size) {
                            var f: Frame? = it
                            while (f != null && f.matchedIndex != -1) {
                                add(f.matchedIndex)
                                f = f.previousFrame
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

    fun head(predicate: T.(lastMatchedIndex: Int, currentIndex: Int) -> Boolean) =
        add { lastMatchedIndex, currentIndex ->
            currentIndex == 0 && predicate(lastMatchedIndex, currentIndex)
        }

    fun head(predicate: T.() -> Boolean) =
        head { _, _ -> predicate() }

    fun after(range: IntRange = 1..1, predicate: T.(lastMatchedIndex: Int, currentIndex: Int) -> Boolean) =
        add { lastMatchedIndex, currentIndex ->
            val distance = currentIndex - lastMatchedIndex

            nextIndex = when {
                distance < range.first -> lastMatchedIndex + range.first
                distance > range.last -> -1
                else -> return@add predicate(lastMatchedIndex, currentIndex)
            }

            false
        }

    fun after(range: IntRange = 1..1, predicate: T.() -> Boolean) =
        after(range) { _, _ -> predicate() }

    fun add(predicate: T.() -> Boolean) = add { _, _ -> predicate() }
}

fun <T> T.declarativePredicate(build: DeclarativePredicateBuilder<T>.() -> Unit) =
    DeclarativePredicateBuilder<T>().apply(build).all(this)

context(_: MatchContext)
fun <T> T.rememberDeclarativePredicate(key: Any, block: DeclarativePredicateBuilder<T>.() -> Unit): Boolean =
    remember(key) { DeclarativePredicateBuilder<T>().apply(block) }.all(this)

context(_: MatchContext)
private fun <T> T.rememberDeclarativePredicate(predicate: context(MatchContext, T) DeclarativePredicateBuilder<T>.() -> Unit) =
    rememberDeclarativePredicate("declarative predicate build") { predicate() }

fun BytecodePatchContext.firstClassDefByDeclarativePredicateOrNull(
    predicate: context(MatchContext, ClassDef) DeclarativePredicateBuilder<ClassDef>.() -> Unit
) = firstClassDefOrNull { rememberDeclarativePredicate(predicate) }

fun BytecodePatchContext.firstClassDefByDeclarativePredicate(
    predicate: context(MatchContext, ClassDef) DeclarativePredicateBuilder<ClassDef>.() -> Unit
) = requireNotNull(firstClassDefByDeclarativePredicateOrNull(predicate))

fun BytecodePatchContext.firstClassDefMutableByDeclarativePredicateOrNull(
    predicate: context(MatchContext, ClassDef) DeclarativePredicateBuilder<ClassDef>.() -> Unit
) = firstClassDefMutableOrNull { rememberDeclarativePredicate(predicate) }

fun BytecodePatchContext.firstClassDefMutableByDeclarativePredicate(
    predicate: context(MatchContext, ClassDef) DeclarativePredicateBuilder<ClassDef>.() -> Unit
) = requireNotNull(firstClassDefMutableByDeclarativePredicateOrNull(predicate))

fun BytecodePatchContext.firstClassDefByDeclarativePredicateOrNull(
    type: String,
    predicate: context(MatchContext, ClassDef) DeclarativePredicateBuilder<ClassDef>.() -> Unit
) = firstClassDefOrNull(type) { rememberDeclarativePredicate(predicate) }

fun BytecodePatchContext.firstClassDefByDeclarativePredicate(
    type: String,
    predicate: context(MatchContext, ClassDef) DeclarativePredicateBuilder<ClassDef>.() -> Unit
) = requireNotNull(firstClassDefByDeclarativePredicateOrNull(type, predicate))

fun BytecodePatchContext.firstClassDefMutableByDeclarativePredicateOrNull(
    type: String,
    predicate: context(MatchContext, ClassDef) DeclarativePredicateBuilder<ClassDef>.() -> Unit
) = firstClassDefMutableOrNull(type) { rememberDeclarativePredicate(predicate) }

fun BytecodePatchContext.firstClassDefMutableByDeclarativePredicate(
    type: String,
    predicate: context(MatchContext, ClassDef) DeclarativePredicateBuilder<ClassDef>.() -> Unit
) = requireNotNull(firstClassDefMutableByDeclarativePredicateOrNull(type, predicate))

fun BytecodePatchContext.firstMethodByDeclarativePredicateOrNull(
    predicate: context(MatchContext, Method) DeclarativePredicateBuilder<Method>.() -> Unit
) = firstMethodOrNull { rememberDeclarativePredicate(predicate) }

fun BytecodePatchContext.firstMethodByDeclarativePredicate(
    predicate: context(MatchContext, Method) DeclarativePredicateBuilder<Method>.() -> Unit
) = requireNotNull(firstMethodByDeclarativePredicateOrNull(predicate))

fun BytecodePatchContext.firstMethodMutableByDeclarativePredicateOrNull(
    predicate: context(MatchContext, Method) DeclarativePredicateBuilder<Method>.() -> Unit
) = firstMethodMutableOrNull { rememberDeclarativePredicate(predicate) }

fun BytecodePatchContext.firstMethodMutableByDeclarativePredicate(
    predicate: context(MatchContext, Method) DeclarativePredicateBuilder<Method>.() -> Unit
) = requireNotNull(firstMethodMutableByDeclarativePredicateOrNull(predicate))

fun BytecodePatchContext.firstMethodByDeclarativePredicateOrNull(
    vararg strings: String,
    predicate: context(MatchContext, Method) DeclarativePredicateBuilder<Method>.() -> Unit
) = firstMethodOrNull(*strings) { rememberDeclarativePredicate(predicate) }

fun BytecodePatchContext.firstMethodByDeclarativePredicate(
    vararg strings: String,
    predicate: context(MatchContext, Method) DeclarativePredicateBuilder<Method>.() -> Unit
) = requireNotNull(firstMethodByDeclarativePredicateOrNull(*strings, predicate = predicate))

fun BytecodePatchContext.firstMethodMutableByDeclarativePredicateOrNull(
    vararg strings: String,
    predicate: context(MatchContext, Method) DeclarativePredicateBuilder<Method>.() -> Unit
) = firstMethodMutableOrNull(*strings) { rememberDeclarativePredicate(predicate) }

fun BytecodePatchContext.firstMethodMutableByDeclarativePredicate(
    vararg strings: String,
    predicate: context(MatchContext, Method) DeclarativePredicateBuilder<Method>.() -> Unit
) = requireNotNull(firstMethodMutableByDeclarativePredicateOrNull(*strings, predicate = predicate))

fun gettingFirstClassDefByDeclarativePredicateOrNull(
    type: String,
    predicate: context(MatchContext, ClassDef) DeclarativePredicateBuilder<ClassDef>.() -> Unit
) = gettingFirstClassDefOrNull(type) { rememberDeclarativePredicate(predicate) }

fun gettingFirstClassDefByDeclarativePredicate(
    type: String,
    predicate: context(MatchContext, ClassDef) DeclarativePredicateBuilder<ClassDef>.() -> Unit
) = CachedReadOnlyProperty { firstClassDefByDeclarativePredicate(type, predicate) }

fun gettingFirstClassDefMutableByDeclarativePredicateOrNull(
    type: String,
    predicate: context(MatchContext, ClassDef) DeclarativePredicateBuilder<ClassDef>.() -> Unit
) = gettingFirstClassDefMutableOrNull(type) { rememberDeclarativePredicate(predicate) }

fun gettingFirstClassDefMutableByDeclarativePredicate(
    type: String,
    predicate: context(MatchContext, ClassDef) DeclarativePredicateBuilder<ClassDef>.() -> Unit
) = CachedReadOnlyProperty { firstClassDefMutableByDeclarativePredicate(type, predicate) }

fun gettingFirstClassDefByDeclarativePredicateOrNull(
    predicate: context(MatchContext, ClassDef) DeclarativePredicateBuilder<ClassDef>.() -> Unit
) = gettingFirstClassDefOrNull { rememberDeclarativePredicate(predicate) }

fun gettingFirstClassDefByDeclarativePredicate(
    predicate: context(MatchContext, ClassDef) DeclarativePredicateBuilder<ClassDef>.() -> Unit
) = CachedReadOnlyProperty { firstClassDefByDeclarativePredicate(predicate) }

fun gettingFirstClassDefMutableByDeclarativePredicateOrNull(
    predicate: context(MatchContext, ClassDef) DeclarativePredicateBuilder<ClassDef>.() -> Unit
) = gettingFirstClassDefMutableOrNull { rememberDeclarativePredicate(predicate) }

fun gettingFirstClassDefMutableByDeclarativePredicate(
    predicate: context(MatchContext, ClassDef) DeclarativePredicateBuilder<ClassDef>.() -> Unit
) = CachedReadOnlyProperty { firstClassDefMutableByDeclarativePredicate(predicate) }

fun gettingFirstMethodByDeclarativePredicateOrNull(
    predicate: context(MatchContext, Method) DeclarativePredicateBuilder<Method>.() -> Unit
) = gettingFirstMethodOrNull { rememberDeclarativePredicate(predicate) }

fun gettingFirstMethodByDeclarativePredicate(
    predicate: context(MatchContext, Method) DeclarativePredicateBuilder<Method>.() -> Unit
) = CachedReadOnlyProperty { firstMethodByDeclarativePredicate(predicate = predicate) }

fun gettingFirstMethodMutableByDeclarativePredicateOrNull(
    predicate: context(MatchContext, Method) DeclarativePredicateBuilder<Method>.() -> Unit
) = gettingFirstMethodMutableOrNull { rememberDeclarativePredicate(predicate) }

fun gettingFirstMethodMutableByDeclarativePredicate(
    predicate: context(MatchContext, Method) DeclarativePredicateBuilder<Method>.() -> Unit
) = CachedReadOnlyProperty { firstMethodMutableByDeclarativePredicate(predicate = predicate) }

fun gettingFirstMethodByDeclarativePredicateOrNull(
    vararg strings: String,
    predicate: context(MatchContext, Method) DeclarativePredicateBuilder<Method>.() -> Unit
) = gettingFirstMethodOrNull(*strings) { rememberDeclarativePredicate(predicate) }

fun gettingFirstMethodByDeclarativePredicate(
    vararg strings: String,
    predicate: context(MatchContext, Method) DeclarativePredicateBuilder<Method>.() -> Unit
) = CachedReadOnlyProperty { firstMethodByDeclarativePredicate(*strings, predicate = predicate) }

fun gettingFirstMethodMutableByDeclarativePredicateOrNull(
    vararg strings: String,
    predicate: context(MatchContext, Method) DeclarativePredicateBuilder<Method>.() -> Unit
) = gettingFirstMethodMutableOrNull(*strings) { rememberDeclarativePredicate(predicate) }

fun gettingFirstMethodMutableByDeclarativePredicate(
    vararg strings: String,
    predicate: context(MatchContext, Method) DeclarativePredicateBuilder<Method>.() -> Unit
) = CachedReadOnlyProperty { firstMethodMutableByDeclarativePredicate(*strings, predicate = predicate) }


class DeclarativePredicateBuilder<T> internal constructor() {
    private val children = mutableListOf<T.() -> Boolean>()

    fun anyOf(block: DeclarativePredicateBuilder<T>.() -> Unit) {
        val child = DeclarativePredicateBuilder<T>().apply(block)
        children += { child.children.any { it() } }
    }

    fun predicate(block: T.() -> Boolean) {
        children += block
    }

    fun all(target: T): Boolean = children.all { target.it() }
    fun any(target: T): Boolean = children.all { target.it() }
}

fun firstMethodComposite(
    predicate:
    context(MatchContext, Method, IndexedMatcher<Instruction>) DeclarativePredicateBuilder<Method>.() -> Unit
) = with(indexedMatcher<Instruction>()) { Composition(indices = this.indices) { predicate() } }

fun DeclarativePredicateBuilder<Method>.accessFlags(vararg flags: AccessFlags) {
    predicate { accessFlags(*flags) }
}

fun DeclarativePredicateBuilder<Method>.returns(returnType: String) {
    predicate { this.returnType.startsWith(returnType) }
}

fun DeclarativePredicateBuilder<Method>.parameterTypes(vararg parameterTypes: String) = predicate {
    this.parameterTypes.size == parameterTypes.size && this.parameterTypes.zip(parameterTypes)
        .all { (a, b) -> a.startsWith(b) }
}

context(_: MatchContext, indexedMatcher: IndexedMatcher<Instruction>)
fun DeclarativePredicateBuilder<Method>.instructions(
    build: context(MatchContext, Method) IndexedMatcher<Instruction>.() -> Unit
) = predicate { implementation { indexedMatcher(indexedMatcher.hashCode(), instructions) { build() } } }

context(_: MatchContext)
fun DeclarativePredicateBuilder<Method>.custom(block: context(MatchContext) Method.() -> Boolean) {
    predicate { block() }
}

class Composition internal constructor(
    val indices: List<Int>,
    private val predicate: context(MatchContext, Method) DeclarativePredicateBuilder<Method>.() -> Unit
) {
    private var _methodOrNull: MutableMethod? = null

    context(context: BytecodePatchContext)
    val methodOrNull: MutableMethod?
        get() {
            if (_methodOrNull == null) {
                _methodOrNull = context.firstMethodMutableByDeclarativePredicateOrNull(predicate)
            }

            return _methodOrNull
        }

    context(_: BytecodePatchContext)
    val method get() = requireNotNull(methodOrNull)
}
