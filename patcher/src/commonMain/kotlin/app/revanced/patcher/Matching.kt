@file:Suppress("unused")

package app.revanced.patcher

import app.revanced.patcher.extensions.*
import app.revanced.patcher.patch.BytecodePatchContext
import com.android.tools.smali.dexlib2.AccessFlags
import com.android.tools.smali.dexlib2.HiddenApiRestriction
import com.android.tools.smali.dexlib2.Opcode
import com.android.tools.smali.dexlib2.iface.*
import com.android.tools.smali.dexlib2.iface.Annotation
import com.android.tools.smali.dexlib2.iface.instruction.*
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

fun ClassDef.anyAnnotation(predicate: Annotation.() -> Boolean) =
    annotations.any(predicate)

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

fun BytecodePatchContext.firstClassDefOrNull(
    type: String? = null, predicate: ClassDefPredicate = { true }
) = with(PredicateContext()) {
    if (type == null) classDefs.firstOrNull { it.predicate() }
    else classDefs[type]?.takeIf { it.predicate() }
}

fun BytecodePatchContext.firstClassDef(
    type: String? = null,
    predicate: ClassDefPredicate = { true }
) = requireNotNull(firstClassDefOrNull(type, predicate))

fun BytecodePatchContext.firstClassDefMutableOrNull(
    type: String? = null,
    predicate: ClassDefPredicate = { true }
) = firstClassDefOrNull(type, predicate)?.let { classDefs.getOrReplaceMutable(it) }

fun BytecodePatchContext.firstClassDefMutable(
    type: String? = null,
    predicate: ClassDefPredicate = { true }
) = requireNotNull(firstClassDefMutableOrNull(type, predicate))

fun BytecodePatchContext.firstMethodOrNull(
    vararg strings: String,
    predicate: MethodPredicate = { true },
): Method? = with(PredicateContext()) {
    if (strings.isEmpty())
        return classDefs.asSequence().flatMap { it.methods.asSequence() }.firstOrNull { it.predicate() }

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

fun BytecodePatchContext.firstMethodMutableOrNull(
    vararg strings: String,
    predicate: MethodPredicate = { true },
) = firstMethodOrNull(strings = strings, predicate)?.let { method ->
    firstClassDefMutable(method.definingClass).methods.first {
        MethodUtil.methodSignaturesMatch(method, it)
    }
}

fun BytecodePatchContext.firstMethodMutable(
    vararg strings: String, predicate: MethodPredicate = { true }
) = requireNotNull(firstMethodMutableOrNull(strings = strings, predicate))

fun gettingFirstClassDefOrNull(
    type: String? = null, predicate: ClassDefPredicate = { true }
) = cachedReadOnlyProperty { firstClassDefOrNull(type, predicate) }

fun gettingFirstClassDef(
    type: String? = null, predicate: ClassDefPredicate = { true }
) = cachedReadOnlyProperty { firstClassDef(type, predicate) }

fun gettingFirstClassDefMutableOrNull(
    type: String? = null, predicate: ClassDefPredicate = { true }
) = cachedReadOnlyProperty { firstClassDefMutableOrNull(type, predicate) }

fun gettingFirstClassDefMutable(
    type: String? = null, predicate: ClassDefPredicate = { true }
) = cachedReadOnlyProperty { firstClassDefMutable(type, predicate) }

fun gettingFirstMethodOrNull(
    vararg strings: String,
    predicate: MethodPredicate = { true },
) = cachedReadOnlyProperty { firstMethodOrNull(strings = strings, predicate) }

fun gettingFirstMethod(
    vararg strings: String,
    predicate: MethodPredicate = { true },
) = cachedReadOnlyProperty { firstMethod(strings = strings, predicate) }

fun gettingFirstMethodMutableOrNull(
    vararg strings: String,
    predicate: MethodPredicate = { true },
) = cachedReadOnlyProperty { firstMethodMutableOrNull(strings = strings, predicate) }

fun gettingFirstMethodMutable(
    vararg strings: String,
    predicate: MethodPredicate = { true },
) = cachedReadOnlyProperty { firstMethodMutable(strings = strings, predicate) }

class PredicateContext internal constructor() : MutableMap<Any, Any> by mutableMapOf()

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

context(_: IndexedMatcher<T>)
fun <T> head(
    predicate: T.(lastMatchedIndex: Int, currentIndex: Int) -> Boolean
): T.(Int, Int) -> Boolean = { lastMatchedIndex, currentIndex ->
    currentIndex == 0 && predicate(lastMatchedIndex, currentIndex)
}

context(_: IndexedMatcher<T>)
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

context(_: PredicateContext)
inline operator fun <T, U, reified M : Matcher<T, U>> M.invoke(
    key: Any,
    iterable: Iterable<T>,
    builder: M.() -> Unit
) = remembered(key) { apply(builder) }(iterable)

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

context(context: PredicateContext)

inline fun <reified V : Any> remembered(key: Any, defaultValue: () -> V) =
    context[key] as? V ?: defaultValue().also { context[key] = it }

private fun <T> cachedReadOnlyProperty(block: BytecodePatchContext.(KProperty<*>) -> T) =
    object : ReadOnlyProperty<BytecodePatchContext, T> {
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

private typealias DeclarativeClassDefPredicate = context(PredicateContext, MutableList<ClassDef.() -> Boolean>) () -> Unit

private typealias DeclarativeMethodPredicate = context(PredicateContext, MutableList<Method.() -> Boolean>) () -> Unit

fun <T> T.declarativePredicate(build: context(MutableList<T.() -> Boolean>) () -> Unit) =
    context(mutableListOf<T.() -> Boolean>().apply(build)) {
        all(this)
    }

context(_: PredicateContext)
fun <T> T.rememberedDeclarativePredicate(key: Any, block: context(MutableList<T.() -> Boolean>) () -> Unit) =
    context(remembered(key) { mutableListOf<T.() -> Boolean>().apply(block) }) {
        all(this)
    }

context(_: PredicateContext)
private fun <T> T.rememberedDeclarativePredicate(
    predicate: context(PredicateContext, MutableList<T.() -> Boolean>) () -> Unit
) = rememberedDeclarativePredicate("declarativePredicate") { predicate() }

fun BytecodePatchContext.firstClassDefByDeclarativePredicateOrNull(
    predicate: DeclarativeClassDefPredicate
) = firstClassDefOrNull { rememberedDeclarativePredicate(predicate) }

fun BytecodePatchContext.firstClassDefByDeclarativePredicateOrNull(
    type: String? = null,
    predicate: DeclarativeClassDefPredicate = { }
) = firstClassDefOrNull(type) { rememberedDeclarativePredicate(predicate) }

fun BytecodePatchContext.firstClassDefByDeclarativePredicate(
    type: String? = null,
    predicate: DeclarativeClassDefPredicate = { }
) = requireNotNull(firstClassDefByDeclarativePredicateOrNull(type, predicate))

fun BytecodePatchContext.firstClassDefMutableByDeclarativePredicateOrNull(
    type: String? = null,
    predicate: DeclarativeClassDefPredicate = { }
) = firstClassDefMutableOrNull(type) { rememberedDeclarativePredicate(predicate) }

fun BytecodePatchContext.firstClassDefMutableByDeclarativePredicate(
    type: String? = null,
    predicate: DeclarativeClassDefPredicate = { }
) = requireNotNull(firstClassDefMutableByDeclarativePredicateOrNull(type, predicate))

fun BytecodePatchContext.firstMethodByDeclarativePredicateOrNull(
    vararg strings: String,
    predicate: DeclarativeMethodPredicate = { }
) = firstMethodOrNull(strings = strings) { rememberedDeclarativePredicate(predicate) }

fun BytecodePatchContext.firstMethodByDeclarativePredicate(
    vararg strings: String,
    predicate: DeclarativeMethodPredicate = { }
) = requireNotNull(firstMethodByDeclarativePredicateOrNull(strings = strings, predicate))

fun BytecodePatchContext.firstMethodMutableByDeclarativePredicateOrNull(
    vararg strings: String,
    predicate: DeclarativeMethodPredicate = { }
) = firstMethodMutableOrNull(strings = strings) { rememberedDeclarativePredicate(predicate) }

fun BytecodePatchContext.firstMethodMutableByDeclarativePredicate(
    vararg strings: String,
    predicate: DeclarativeMethodPredicate = { }
) = requireNotNull(firstMethodMutableByDeclarativePredicateOrNull(strings = strings, predicate))

fun gettingFirstClassDefByDeclarativePredicateOrNull(
    type: String? = null,
    predicate: DeclarativeClassDefPredicate = { }
) = gettingFirstClassDefOrNull(type) { rememberedDeclarativePredicate(predicate) }

fun gettingFirstClassDefByDeclarativePredicate(
    type: String? = null,
    predicate: DeclarativeClassDefPredicate = { }
) = cachedReadOnlyProperty { firstClassDefByDeclarativePredicate(type, predicate) }

fun gettingFirstClassDefMutableByDeclarativePredicateOrNull(
    type: String? = null,
    predicate: DeclarativeClassDefPredicate = { }
) = gettingFirstClassDefMutableOrNull(type) { rememberedDeclarativePredicate(predicate) }

fun gettingFirstClassDefMutableByDeclarativePredicate(
    type: String? = null,
    predicate: DeclarativeClassDefPredicate = { }
) = cachedReadOnlyProperty { firstClassDefMutableByDeclarativePredicate(type, predicate) }

fun gettingFirstMethodByDeclarativePredicateOrNull(
    vararg strings: String,
    predicate: DeclarativeMethodPredicate = { }
) = gettingFirstMethodOrNull(strings = strings) { rememberedDeclarativePredicate(predicate) }

fun gettingFirstMethodByDeclarativePredicate(
    vararg strings: String,
    predicate: DeclarativeMethodPredicate = { }
) = cachedReadOnlyProperty { firstMethodByDeclarativePredicate(strings = strings, predicate) }

fun gettingFirstMethodMutableByDeclarativePredicateOrNull(
    vararg strings: String,
    predicate: DeclarativeMethodPredicate = { }
) = gettingFirstMethodMutableOrNull(strings = strings) { rememberedDeclarativePredicate(predicate) }

fun gettingFirstMethodMutableByDeclarativePredicate(
    vararg strings: String,
    predicate: DeclarativeMethodPredicate = { }
) = cachedReadOnlyProperty { firstMethodMutableByDeclarativePredicate(strings = strings, predicate) }

context(list: MutableList<T.() -> Boolean>)
fun <T> allOf(block: MutableList<T.() -> Boolean>.() -> Unit) {
    val child = mutableListOf<T.() -> Boolean>().apply(block)
    list.add { child.all { it() } }
}

context(list: MutableList<T.() -> Boolean>)
fun <T> anyOf(block: MutableList<T.() -> Boolean>.() -> Unit) {
    val child = mutableListOf<T.() -> Boolean>().apply(block)
    list.add { child.any { it() } }
}

context(list: MutableList<T.() -> Boolean>)
fun <T> predicate(block: T.() -> Boolean) {
    list.add(block)
}

context(list: MutableList<T.() -> Boolean>)
fun <T> all(target: T): Boolean = list.all { target.it() }

context(list: MutableList<T.() -> Boolean>)
fun <T> any(target: T): Boolean = list.any { target.it() }

context(_: MutableList<Method.() -> Boolean>)
fun accessFlags(vararg flags: AccessFlags) =
    predicate { accessFlags(flags = flags) }

context(_: MutableList<Method.() -> Boolean>)
fun returnType(
    returnType: String,
    compare: String.(String) -> Boolean = String::startsWith
) = predicate { this.returnType.compare(returnType) }

context(_: MutableList<Method.() -> Boolean>)
fun name(
    name: String,
    compare: String.(String) -> Boolean = String::equals
) = predicate { this.name.compare(name) }

context(_: MutableList<Method.() -> Boolean>)
fun definingClass(
    definingClass: String,
    compare: String.(String) -> Boolean = String::equals
) = predicate { this.definingClass.compare(definingClass) }

context(_: MutableList<Method.() -> Boolean>)
fun parameterTypes(vararg parameterTypePrefixes: String) = predicate {
    parameterTypes.size == parameterTypePrefixes.size && parameterTypes.zip(parameterTypePrefixes)
        .all { (a, b) -> a.startsWith(b) }
}

context(_: MutableList<Method.() -> Boolean>, matcher: IndexedMatcher<Instruction>)
fun instructions(
    build: context(IndexedMatcher<Instruction>) () -> Unit
) {
    build()
    predicate { implementation { matcher(instructions) } }
}

context(_: MutableList<Method.() -> Boolean>, matcher: IndexedMatcher<Instruction>)
fun instructions(
    vararg predicates: Instruction.(currentIndex: Int, lastMatchedIndex: Int) -> Boolean
) = instructions {
    predicates.forEach { +it }
}

context(_: MutableList<Method.() -> Boolean>)
fun custom(block: Method.() -> Boolean) {
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

fun firstMethodBuilder(
    vararg strings: String,
    builder:
    context(PredicateContext, MutableList<Method.() -> Boolean>, IndexedMatcher<Instruction>, MutableList<String>)() -> Unit
) = Match(strings = strings, builder)

class Match private constructor(
    private val strings: MutableList<String>,
    indexedMatcher: IndexedMatcher<Instruction> = indexedMatcher(),
    build: context(
    PredicateContext, MutableList<Method.() -> Boolean>,
    IndexedMatcher<Instruction>, MutableList<String>) () -> Unit
) {
    internal constructor(
        vararg strings: String,
        builder: context(
        PredicateContext, MutableList<Method.() -> Boolean>,
        IndexedMatcher<Instruction>, MutableList<String>) () -> Unit
    ) : this(strings = mutableListOf(elements = strings), build = builder)

    private val methodOrNullMap = HashMap<BytecodePatchContext, MutableMethod?>(1)

    private val predicate: DeclarativeMethodPredicate = context(strings, indexedMatcher) { { build() } }

    context(context: BytecodePatchContext)

    val methodOrNull: MutableMethod?
        get() = if (context in methodOrNullMap) methodOrNullMap[context]
        else methodOrNullMap.getOrPut(context) {
            context.firstMethodMutableByDeclarativePredicateOrNull(
                strings = strings.toTypedArray(),
                predicate
            )
        }

    context(_: BytecodePatchContext)
    val method get() = requireNotNull(methodOrNull)

    context(context: BytecodePatchContext)
    val classDefOrNull get() = methodOrNull?.definingClass?.let(context::firstClassDefOrNull)

    context(_: BytecodePatchContext)
    val classDef get() = requireNotNull(classDefOrNull)

    val indices = indexedMatcher.indices
}
