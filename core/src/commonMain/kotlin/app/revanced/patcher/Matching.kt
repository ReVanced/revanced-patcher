@file:Suppress("unused", "MemberVisibilityCanBePrivate", "CONTEXT_RECEIVERS_DEPRECATED")

package app.revanced.patcher

import app.revanced.patcher.extensions.*
import app.revanced.patcher.patch.BytecodePatchContext
import com.android.tools.smali.dexlib2.AccessFlags
import com.android.tools.smali.dexlib2.HiddenApiRestriction
import com.android.tools.smali.dexlib2.Opcode
import com.android.tools.smali.dexlib2.iface.*
import com.android.tools.smali.dexlib2.iface.Annotation
import com.android.tools.smali.dexlib2.iface.instruction.*
import com.android.tools.smali.dexlib2.iface.instruction.formats.Instruction22t
import com.android.tools.smali.dexlib2.util.MethodUtil
import com.sun.jdi.StringReference
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

fun BytecodePatchContext.firstClassDefOrNull(predicate: context(PredicateContext) ClassDef.() -> Boolean) =
    with(PredicateContext()) { classDefs.firstOrNull { it.predicate() } }

fun BytecodePatchContext.firstClassDef(predicate: context(PredicateContext) ClassDef.() -> Boolean) =
    requireNotNull(firstClassDefOrNull(predicate))

fun BytecodePatchContext.firstClassDefMutableOrNull(predicate: context(PredicateContext) ClassDef.() -> Boolean) =
    firstClassDefOrNull(predicate)?.let { classDefs.getOrReplaceMutable(it) }

fun BytecodePatchContext.firstClassDefMutable(predicate: context(PredicateContext) ClassDef.() -> Boolean) =
    requireNotNull(firstClassDefMutableOrNull(predicate))

fun BytecodePatchContext.firstClassDefOrNull(
    type: String, predicate: (context(PredicateContext) ClassDef.() -> Boolean)? = null
) = classDefs[type]?.takeIf {
    predicate == null || with(PredicateContext()) { it.predicate() }
}

fun BytecodePatchContext.firstClassDef(
    type: String,
    predicate: (context(PredicateContext) ClassDef.() -> Boolean)? = null
) = requireNotNull(firstClassDefOrNull(type, predicate))

fun BytecodePatchContext.firstClassDefMutableOrNull(
    type: String,
    predicate: (context(PredicateContext) ClassDef.() -> Boolean)? = null
) = firstClassDefOrNull(type, predicate)?.let { classDefs.getOrReplaceMutable(it) }

fun BytecodePatchContext.firstClassDefMutable(
    type: String,
    predicate: (context(PredicateContext) ClassDef.() -> Boolean)? = null
) = requireNotNull(firstClassDefMutableOrNull(type, predicate))

fun BytecodePatchContext.firstMethodOrNull(predicate: context(PredicateContext) Method.() -> Boolean): Method? {
    val methods = classDefs.asSequence().flatMap { it.methods.asSequence() }
    with(PredicateContext()) {
        return methods.firstOrNull { it.predicate() }
    }
}

fun BytecodePatchContext.firstMethod(predicate: context(PredicateContext) Method.() -> Boolean) =
    requireNotNull(firstMethodOrNull(predicate))

fun BytecodePatchContext.firstMethodMutableOrNull(predicate: context(PredicateContext) Method.() -> Boolean) =
    firstMethodOrNull(predicate)?.let { method ->
        firstClassDefMutable(method.definingClass).methods.first {
            MethodUtil.methodSignaturesMatch(method, it)
        }
    }

fun BytecodePatchContext.firstMethodMutable(predicate: context(PredicateContext) Method.() -> Boolean) =
    requireNotNull(firstMethodMutableOrNull(predicate))

fun BytecodePatchContext.firstMethodOrNull(
    vararg strings: String,
    predicate: context(PredicateContext) Method.() -> Boolean = { true },
) = with(PredicateContext()) {
    val methodsWithStrings = strings.mapNotNull { classDefs.methodsByString[it] }
    if (methodsWithStrings.size != strings.size) return null

    methodsWithStrings.minBy { it.size }.firstOrNull { method ->
        val containsAllOtherStrings = methodsWithStrings.all { method in it }
        containsAllOtherStrings && method.predicate()
    }
}

fun BytecodePatchContext.firstMethod(
    vararg strings: String,
    predicate: context(PredicateContext) Method.() -> Boolean = { true },
) = requireNotNull(firstMethodOrNull(*strings, predicate = predicate))

fun BytecodePatchContext.firstMethodMutableOrNull(
    vararg strings: String,
    predicate: context(PredicateContext) Method.() -> Boolean = { true },
) = firstMethodOrNull(*strings, predicate = predicate)?.let { method ->
    firstClassDefMutable(method.definingClass).methods.first {
        MethodUtil.methodSignaturesMatch(
            method, it
        )
    }
}

fun BytecodePatchContext.firstMethodMutable(
    vararg strings: String, predicate: context(PredicateContext) Method.() -> Boolean = { true }
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

fun gettingFirstClassDefOrNull(predicate: context(PredicateContext) ClassDef.() -> Boolean) =
    CachedReadOnlyProperty { firstClassDefOrNull(predicate) }

fun gettingFirstClassDef(predicate: context(PredicateContext) ClassDef.() -> Boolean) =
    CachedReadOnlyProperty { firstClassDef(predicate) }

fun gettingFirstClassDefMutableOrNull(predicate: context(PredicateContext) ClassDef.() -> Boolean) =
    CachedReadOnlyProperty { firstClassDefMutableOrNull(predicate) }

fun gettingFirstClassDefMutable(predicate: context(PredicateContext) ClassDef.() -> Boolean) =
    CachedReadOnlyProperty { firstClassDefMutable(predicate) }

fun gettingFirstClassDefOrNull(
    type: String, predicate: (context(PredicateContext) ClassDef.() -> Boolean)? = null
) = CachedReadOnlyProperty { firstClassDefOrNull(type, predicate) }

fun gettingFirstClassDef(
    type: String, predicate: (context(PredicateContext) ClassDef.() -> Boolean)? = null
) = CachedReadOnlyProperty { firstClassDef(type, predicate) }

fun gettingFirstClassDefMutableOrNull(
    type: String, predicate: (context(PredicateContext) ClassDef.() -> Boolean)? = null
) = CachedReadOnlyProperty { firstClassDefMutableOrNull(type, predicate) }

fun gettingFirstClassDefMutable(
    type: String, predicate: (context(PredicateContext) ClassDef.() -> Boolean)? = null
) = CachedReadOnlyProperty { firstClassDefMutable(type, predicate) }

fun gettingFirstMethodOrNull(predicate: context(PredicateContext) Method.() -> Boolean) =
    CachedReadOnlyProperty { firstMethodOrNull(predicate) }

fun gettingFirstMethod(predicate: context(PredicateContext) Method.() -> Boolean) =
    CachedReadOnlyProperty { firstMethod(predicate) }

fun gettingFirstMethodMutableOrNull(predicate: context(PredicateContext) Method.() -> Boolean) =
    CachedReadOnlyProperty { firstMethodMutableOrNull(predicate) }

fun gettingFirstMethodMutable(predicate: context(PredicateContext) Method.() -> Boolean) =
    CachedReadOnlyProperty { firstMethodMutable(predicate) }

fun gettingFirstMethodOrNull(
    vararg strings: String,
    predicate: context(PredicateContext) Method.() -> Boolean = { true },
) = CachedReadOnlyProperty { firstMethodOrNull(*strings, predicate = predicate) }

fun gettingFirstMethod(
    vararg strings: String,
    predicate: context(PredicateContext) Method.() -> Boolean = { true },
) = CachedReadOnlyProperty { firstMethod(*strings, predicate = predicate) }

fun gettingFirstMethodMutableOrNull(
    vararg strings: String,
    predicate: context(PredicateContext) Method.() -> Boolean = { true },
) = CachedReadOnlyProperty { firstMethodMutableOrNull(*strings, predicate = predicate) }

fun gettingFirstMethodMutable(
    vararg strings: String,
    predicate: context(PredicateContext) Method.() -> Boolean = { true },
) = CachedReadOnlyProperty { firstMethodMutable(*strings, predicate = predicate) }

// region Matcher

// region IndexedMatcher

fun <T> indexedMatcher() = IndexedMatcher<T>()

fun <T> indexedMatcher(build: IndexedMatcher<T>.() -> Unit) =
    IndexedMatcher<T>().apply(build)

fun <T> Iterable<T>.matchIndexed(build: IndexedMatcher<T>.() -> Unit) =
    indexedMatcher(build)(this)

context(_: PredicateContext)
fun <T> Iterable<T>.rememberedMatchIndexed(key: Any, build: IndexedMatcher<T>.() -> Unit) =
    indexedMatcher<T>()(key, this, build)

context(matcher: IndexedMatcher<T>)
fun <T> head(
    predicate: T.(lastMatchedIndex: Int, currentIndex: Int) -> Boolean
): T.(Int, Int) -> Boolean = { lastMatchedIndex, currentIndex ->
    currentIndex == 0 && predicate(lastMatchedIndex, currentIndex)
}

context(matcher: IndexedMatcher<T>)
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

context(matcher: IndexedMatcher<T>)
fun <T> after(range: IntRange = 1..1, predicate: T.() -> Boolean) =
    after(range) { _, _ -> predicate() }

context(matcher: IndexedMatcher<T>)
operator fun <T> (T.(Int, Int) -> Boolean).unaryPlus() = matcher.add(this)

class IndexedMatcher<T> : Matcher<T, T.(lastMatchedIndex: Int, currentIndex: Int) -> Boolean>() {
    private val _indices: MutableList<Int> = mutableListOf()
    val indices: List<Int> = _indices

    private var lastMatchedIndex = -1
    private var currentIndex = -1
    var nextIndex: Int? = null

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
}

// endregion

context(_: PredicateContext)
inline operator fun <T, U, reified M : Matcher<T, U>> M.invoke(key: Any, iterable: Iterable<T>, builder: M.() -> Unit) =
    remembered(key) { apply(builder) }(iterable)

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

class PredicateContext internal constructor() : MutableMap<Any, Any> by mutableMapOf()

context(context: PredicateContext)
inline fun <reified V : Any> remembered(key: Any, defaultValue: () -> V) =
    context[key] as? V ?: defaultValue().also { context[key] = it }


fun <T> T.declarativePredicate(build: DeclarativePredicateBuilder<T>.() -> Unit) =
    DeclarativePredicateBuilder<T>().apply(build).all(this)

context(_: PredicateContext)
fun <T> T.rememberedDeclarativePredicate(key: Any, block: DeclarativePredicateBuilder<T>.() -> Unit): Boolean =
    remembered(key) { DeclarativePredicateBuilder<T>().apply(block) }.all(this)

context(_: PredicateContext)
private fun <T> T.rememberedDeclarativePredicate(predicate: context(PredicateContext, T) DeclarativePredicateBuilder<T>.() -> Unit) =
    rememberedDeclarativePredicate("declarative predicate build") { predicate() }

fun BytecodePatchContext.firstClassDefByDeclarativePredicateOrNull(
    predicate: context(PredicateContext, ClassDef) DeclarativePredicateBuilder<ClassDef>.() -> Unit
) = firstClassDefOrNull { rememberedDeclarativePredicate(predicate) }

fun BytecodePatchContext.firstClassDefByDeclarativePredicate(
    predicate: context(PredicateContext, ClassDef) DeclarativePredicateBuilder<ClassDef>.() -> Unit
) = requireNotNull(firstClassDefByDeclarativePredicateOrNull(predicate))

fun BytecodePatchContext.firstClassDefMutableByDeclarativePredicateOrNull(
    predicate: context(PredicateContext, ClassDef) DeclarativePredicateBuilder<ClassDef>.() -> Unit
) = firstClassDefMutableOrNull { rememberedDeclarativePredicate(predicate) }

fun BytecodePatchContext.firstClassDefMutableByDeclarativePredicate(
    predicate: context(PredicateContext, ClassDef) DeclarativePredicateBuilder<ClassDef>.() -> Unit
) = requireNotNull(firstClassDefMutableByDeclarativePredicateOrNull(predicate))

fun BytecodePatchContext.firstClassDefByDeclarativePredicateOrNull(
    type: String,
    predicate: context(PredicateContext, ClassDef) DeclarativePredicateBuilder<ClassDef>.() -> Unit
) = firstClassDefOrNull(type) { rememberedDeclarativePredicate(predicate) }

fun BytecodePatchContext.firstClassDefByDeclarativePredicate(
    type: String,
    predicate: context(PredicateContext, ClassDef) DeclarativePredicateBuilder<ClassDef>.() -> Unit
) = requireNotNull(firstClassDefByDeclarativePredicateOrNull(type, predicate))

fun BytecodePatchContext.firstClassDefMutableByDeclarativePredicateOrNull(
    type: String,
    predicate: context(PredicateContext, ClassDef) DeclarativePredicateBuilder<ClassDef>.() -> Unit
) = firstClassDefMutableOrNull(type) { rememberedDeclarativePredicate(predicate) }

fun BytecodePatchContext.firstClassDefMutableByDeclarativePredicate(
    type: String,
    predicate: context(PredicateContext, ClassDef) DeclarativePredicateBuilder<ClassDef>.() -> Unit
) = requireNotNull(firstClassDefMutableByDeclarativePredicateOrNull(type, predicate))

fun BytecodePatchContext.firstMethodByDeclarativePredicateOrNull(
    predicate: context(PredicateContext, Method) DeclarativePredicateBuilder<Method>.() -> Unit
) = firstMethodOrNull { rememberedDeclarativePredicate(predicate) }

fun BytecodePatchContext.firstMethodByDeclarativePredicate(
    predicate: context(PredicateContext, Method) DeclarativePredicateBuilder<Method>.() -> Unit
) = requireNotNull(firstMethodByDeclarativePredicateOrNull(predicate))

fun BytecodePatchContext.firstMethodMutableByDeclarativePredicateOrNull(
    predicate: context(PredicateContext, Method) DeclarativePredicateBuilder<Method>.() -> Unit
) = firstMethodMutableOrNull { rememberedDeclarativePredicate(predicate) }

fun BytecodePatchContext.firstMethodMutableByDeclarativePredicate(
    predicate: context(PredicateContext, Method) DeclarativePredicateBuilder<Method>.() -> Unit
) = requireNotNull(firstMethodMutableByDeclarativePredicateOrNull(predicate))

fun BytecodePatchContext.firstMethodByDeclarativePredicateOrNull(
    vararg strings: String,
    predicate: context(PredicateContext, Method) DeclarativePredicateBuilder<Method>.() -> Unit
) = firstMethodOrNull(*strings) { rememberedDeclarativePredicate(predicate) }

fun BytecodePatchContext.firstMethodByDeclarativePredicate(
    vararg strings: String,
    predicate: context(PredicateContext, Method) DeclarativePredicateBuilder<Method>.() -> Unit
) = requireNotNull(firstMethodByDeclarativePredicateOrNull(*strings, predicate = predicate))

fun BytecodePatchContext.firstMethodMutableByDeclarativePredicateOrNull(
    vararg strings: String,
    predicate: context(PredicateContext, Method) DeclarativePredicateBuilder<Method>.() -> Unit
) = firstMethodMutableOrNull(*strings) { rememberedDeclarativePredicate(predicate) }

fun BytecodePatchContext.firstMethodMutableByDeclarativePredicate(
    vararg strings: String,
    predicate: context(PredicateContext, Method) DeclarativePredicateBuilder<Method>.() -> Unit
) = requireNotNull(firstMethodMutableByDeclarativePredicateOrNull(*strings, predicate = predicate))

fun gettingFirstClassDefByDeclarativePredicateOrNull(
    type: String,
    predicate: context(PredicateContext, ClassDef) DeclarativePredicateBuilder<ClassDef>.() -> Unit
) = gettingFirstClassDefOrNull(type) { rememberedDeclarativePredicate(predicate) }

fun gettingFirstClassDefByDeclarativePredicate(
    type: String,
    predicate: context(PredicateContext, ClassDef) DeclarativePredicateBuilder<ClassDef>.() -> Unit
) = CachedReadOnlyProperty { firstClassDefByDeclarativePredicate(type, predicate) }

fun gettingFirstClassDefMutableByDeclarativePredicateOrNull(
    type: String,
    predicate: context(PredicateContext, ClassDef) DeclarativePredicateBuilder<ClassDef>.() -> Unit
) = gettingFirstClassDefMutableOrNull(type) { rememberedDeclarativePredicate(predicate) }

fun gettingFirstClassDefMutableByDeclarativePredicate(
    type: String,
    predicate: context(PredicateContext, ClassDef) DeclarativePredicateBuilder<ClassDef>.() -> Unit
) = CachedReadOnlyProperty { firstClassDefMutableByDeclarativePredicate(type, predicate) }

fun gettingFirstClassDefByDeclarativePredicateOrNull(
    predicate: context(PredicateContext, ClassDef) DeclarativePredicateBuilder<ClassDef>.() -> Unit
) = gettingFirstClassDefOrNull { rememberedDeclarativePredicate(predicate) }

fun gettingFirstClassDefByDeclarativePredicate(
    predicate: context(PredicateContext, ClassDef) DeclarativePredicateBuilder<ClassDef>.() -> Unit
) = CachedReadOnlyProperty { firstClassDefByDeclarativePredicate(predicate) }

fun gettingFirstClassDefMutableByDeclarativePredicateOrNull(
    predicate: context(PredicateContext, ClassDef) DeclarativePredicateBuilder<ClassDef>.() -> Unit
) = gettingFirstClassDefMutableOrNull { rememberedDeclarativePredicate(predicate) }

fun gettingFirstClassDefMutableByDeclarativePredicate(
    predicate: context(PredicateContext, ClassDef) DeclarativePredicateBuilder<ClassDef>.() -> Unit
) = CachedReadOnlyProperty { firstClassDefMutableByDeclarativePredicate(predicate) }

fun gettingFirstMethodByDeclarativePredicateOrNull(
    predicate: context(PredicateContext, Method) DeclarativePredicateBuilder<Method>.() -> Unit
) = gettingFirstMethodOrNull { rememberedDeclarativePredicate(predicate) }

fun gettingFirstMethodByDeclarativePredicate(
    predicate: context(PredicateContext, Method) DeclarativePredicateBuilder<Method>.() -> Unit
) = CachedReadOnlyProperty { firstMethodByDeclarativePredicate(predicate = predicate) }

fun gettingFirstMethodMutableByDeclarativePredicateOrNull(
    predicate: context(PredicateContext, Method) DeclarativePredicateBuilder<Method>.() -> Unit
) = gettingFirstMethodMutableOrNull { rememberedDeclarativePredicate(predicate) }

fun gettingFirstMethodMutableByDeclarativePredicate(
    predicate: context(PredicateContext, Method) DeclarativePredicateBuilder<Method>.() -> Unit
) = CachedReadOnlyProperty { firstMethodMutableByDeclarativePredicate(predicate = predicate) }

fun gettingFirstMethodByDeclarativePredicateOrNull(
    vararg strings: String,
    predicate: context(PredicateContext, Method) DeclarativePredicateBuilder<Method>.() -> Unit
) = gettingFirstMethodOrNull(*strings) { rememberedDeclarativePredicate(predicate) }

fun gettingFirstMethodByDeclarativePredicate(
    vararg strings: String,
    predicate: context(PredicateContext, Method) DeclarativePredicateBuilder<Method>.() -> Unit
) = CachedReadOnlyProperty { firstMethodByDeclarativePredicate(*strings, predicate = predicate) }

fun gettingFirstMethodMutableByDeclarativePredicateOrNull(
    vararg strings: String,
    predicate: context(PredicateContext, Method) DeclarativePredicateBuilder<Method>.() -> Unit
) = gettingFirstMethodMutableOrNull(*strings) { rememberedDeclarativePredicate(predicate) }

fun gettingFirstMethodMutableByDeclarativePredicate(
    vararg strings: String,
    predicate: context(PredicateContext, Method) DeclarativePredicateBuilder<Method>.() -> Unit
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
    vararg strings: String,
    builder:
    context(PredicateContext, Method, IndexedMatcher<Instruction>, MutableList<String>) DeclarativePredicateBuilder<Method>.() -> Unit
) = with(indexedMatcher<Instruction>()) matcher@{
    with(mutableListOf<String>()) strings@{
        addAll(strings)

        Composition(
            indices = this@matcher.indices,
            strings = this@strings
        ) { builder() }
    }
}

val m = firstMethodComposite("lookup") {
    instructions(
        head { string == "str" },
        anyOf(),
        anyOf(after(1..2, string("also lookup"))),
        string("s", String::startsWith),
        string(),
        literal(),
        after(1..4, anyOf()),
        noneOf(`is`<Instruction22t>()),
        instruction { opcode == Opcode.CONST_STRING },
        { _, _ -> opcode == Opcode.CONST_STRING },
    )
    instructions {
        +head(literal())

        if (true)
            +after(1..2, string("lookup"))
        else
            +instruction { opcode == Opcode.CONST_STRING }

        add { currentIndex, lastMatchedIndex ->
            currentIndex == 2 && opcode == Opcode.CONST_STRING
        }
    }

    instructions {
        +anyOf(after(1..2, string("also lookup")), Opcode.IF_EQ())
        +head(anyOf(string("s"), "s"(), Opcode.IF_EQ()))
        +head(allOf(Opcode.CONST_STRING_JUMBO(), "str"()))
        add(instruction { this.opcode == Opcode.CONST_STRING || this.string == "lookup" })
        add(instruction { string == "lookup" })
        +after(1..2, anyOf(string("s"), Opcode.IF_EQ()))
        +string("also lookup")
        +"equals"()
        +"prefix" { startsWith(it) }
        +string { startsWith("prefix") }
        +"suffix"(String::endsWith)
        +literal(12) { it >= this }
        +literal(1232)
        +literal { this >= 1123 }
        +literal()
        +string()
        +string { startsWith("s") }
        +method()
        +reference()
        +field()
        +`is`<Instruction22t>()
        +`is`<WideLiteralInstruction>()
        +allOf(`is`<ReferenceInstruction>(), string("test"))
        +`is`<ReferenceInstruction> { reference !is StringReference }
        +`is`<VariableRegisterInstruction> { registerCount > 2 }
        +registers(0, 1, 1, 2)
        +noneOf(registers({ size > 3 }), reference { contains("SomeClass") })
        +type()
        +Opcode.CONST_STRING()
        +after(1..2, Opcode.RETURN_VOID())
        +reference { startsWith("some") }
        +field("s")
        +allOf() // Wildcard
        +anyOf(noneOf(string(), literal(123)), allOf(Opcode.CONST_STRING(), string("tet")))
        +method("abc") { startsWith(it) }
        +after(1..2, string("also lookup") { startsWith(it) })
        +after(1..2, anyOf(string("a", String::endsWith), Opcode.CONST_4()))
        +reference { contains("some") }
        +method("name")
        +field("name")
        +after(1..2, reference("com/example", String::contains))
        +after(1..2, reference("lookup()V", String::endsWith))
        +after(1..2, reference("Lcom/example;->method()V", String::startsWith))
        +after(1..2, reference("Lcom/example;->method()V"))
        +after(1..2, reference("Lcom/example;->field:Ljava/lang/String;") { endsWith(it) })
    }
}

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

context(stringsList: MutableList<String>, builder: DeclarativePredicateBuilder<Method>)
fun string(
    string: String,
    compare: String.(String) -> Boolean = String::equals
): Instruction.(Int, Int) -> Boolean {
    if (compare == String::equals) stringsList += string

    return string { compare(string) }
}

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

fun DeclarativePredicateBuilder<Method>.accessFlags(vararg flags: AccessFlags) =
    predicate { accessFlags(*flags) }

fun DeclarativePredicateBuilder<Method>.returnType(
    returnType: String,
    compare: String.(String) -> Boolean = String::startsWith
) = predicate { this.returnType.compare(returnType) }

fun DeclarativePredicateBuilder<Method>.name(
    name: String,
    compare: String.(String) -> Boolean = String::equals
) =
    predicate { this.name.compare(name) }

fun DeclarativePredicateBuilder<Method>.definingClass(
    definingClass: String,
    compare: String.(String) -> Boolean = String::equals
) = predicate { this.definingClass.compare(definingClass) }

fun DeclarativePredicateBuilder<Method>.parameterTypes(vararg parameterTypePrefixes: String) = predicate {
    parameterTypes.size == parameterTypePrefixes.size && parameterTypes.zip(parameterTypePrefixes)
        .all { (a, b) -> a.startsWith(b) }
}

context(matcher: IndexedMatcher<Instruction>)
fun DeclarativePredicateBuilder<Method>.instructions(
    build: IndexedMatcher<Instruction>.() -> Unit
) {
    matcher.apply(build)
    predicate { implementation { matcher(instructions) } }
}

context(matcher: IndexedMatcher<Instruction>)
fun DeclarativePredicateBuilder<Method>.instructions(
    vararg predicates: Instruction.(currentIndex: Int, lastMatchedIndex: Int) -> Boolean
) = instructions { addAll(predicates) }

fun DeclarativePredicateBuilder<Method>.custom(block: Method.() -> Boolean) {
    predicate { block() }
}

class Composition internal constructor(
    val indices: List<Int>,
    val strings: List<String>,
    private val predicate: context(PredicateContext, Method) DeclarativePredicateBuilder<Method>.() -> Unit
) {
    private var _methodOrNull: com.android.tools.smali.dexlib2.mutable.MutableMethod? = null

    context(context: BytecodePatchContext)
    val methodOrNull: com.android.tools.smali.dexlib2.mutable.MutableMethod?
        get() {
            if (_methodOrNull == null) {
                _methodOrNull = if (strings.isEmpty())
                    context.firstMethodMutableByDeclarativePredicateOrNull(predicate)
                else
                    context.firstMethodMutableByDeclarativePredicateOrNull(
                        strings = strings.toTypedArray(),
                        predicate
                    )
            }

            return _methodOrNull
        }

    context(_: BytecodePatchContext)
    val method get() = requireNotNull(methodOrNull)
}
