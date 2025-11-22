@file:Suppress("unused", "MemberVisibilityCanBePrivate", "CONTEXT_RECEIVERS_DEPRECATED")

package app.revanced.patcher

import app.revanced.patcher.Matcher.MatchContext
import app.revanced.patcher.dex.mutable.MutableMethod
import app.revanced.patcher.extensions.*
import app.revanced.patcher.patch.BytecodePatchContext
import com.android.tools.smali.dexlib2.AccessFlags
import com.android.tools.smali.dexlib2.HiddenApiRestriction
import com.android.tools.smali.dexlib2.Opcode
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

context(context: BytecodePatchContext)
fun Iterable<ClassDef>.firstMethodMutableOrNull(predicate: context(MatchContext) Method.() -> Boolean): MutableMethod? =
    with(context) {
        with(MatchContext()) {
            this@firstMethodMutableOrNull.forEach { classDef ->
                classDef.methods.firstOrNull { it.predicate() }?.let { method ->
                    return classDef.mutable().methods.first { MethodUtil.methodSignaturesMatch(it, method) }
                }
            }

            null
        }
    }

context(_: BytecodePatchContext)
fun Iterable<ClassDef>.firstMethodMutable(predicate: context(MatchContext) Method.() -> Boolean) =
    requireNotNull(firstMethodMutableOrNull(predicate))

fun BytecodePatchContext.firstMethodOrNull(predicate: context(MatchContext) Method.() -> Boolean) =
    classDefs.firstMethodOrNull(predicate)

fun BytecodePatchContext.firstMethod(predicate: context(MatchContext) Method.() -> Boolean) =
    requireNotNull(firstMethodOrNull(predicate))


fun BytecodePatchContext.firstMethodMutableOrNull(predicate: context(MatchContext) Method.() -> Boolean) =
    classDefs.firstMethodMutableOrNull(predicate)

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

inline fun <reified C, T> ReadOnlyProperty(crossinline block: C.(KProperty<*>) -> T) =
    ReadOnlyProperty<Any?, T> { thisRef, property ->
        require(thisRef is C)

        thisRef.block(property)
    }

fun gettingFirstClassDefOrNull(predicate: context(MatchContext) ClassDef.() -> Boolean) =
    ReadOnlyProperty<BytecodePatchContext, ClassDef?> { firstClassDefOrNull(predicate) }

fun gettingFirstClassDef(predicate: context(MatchContext) ClassDef.() -> Boolean) =
    requireNotNull(gettingFirstClassDefOrNull(predicate))

fun gettingFirstClassDefMutableOrNull(predicate: context(MatchContext) ClassDef.() -> Boolean) =
    ReadOnlyProperty<BytecodePatchContext, ClassDef?> { firstClassDefMutableOrNull(predicate) }

fun gettingFirstClassDefMutable(predicate: context(MatchContext) ClassDef.() -> Boolean) =
    requireNotNull(gettingFirstClassDefMutableOrNull(predicate))

fun gettingFirstClassDefOrNull(
    type: String, predicate: (context(MatchContext) ClassDef.() -> Boolean)? = null
) = ReadOnlyProperty<BytecodePatchContext, ClassDef?> { firstClassDefOrNull(type, predicate) }

fun gettingFirstClassDef(
    type: String, predicate: (context(MatchContext) ClassDef.() -> Boolean)? = null
) = requireNotNull(gettingFirstClassDefOrNull(type, predicate))

fun gettingFirstClassDefMutableOrNull(
    type: String, predicate: (context(MatchContext) ClassDef.() -> Boolean)? = null
) = ReadOnlyProperty<BytecodePatchContext, ClassDef?> { firstClassDefMutableOrNull(type, predicate) }

fun gettingFirstClassDefMutable(
    type: String, predicate: (context(MatchContext) ClassDef.() -> Boolean)? = null
) = requireNotNull(gettingFirstClassDefMutableOrNull(type, predicate))

fun gettingFirstMethodOrNull(predicate: context(MatchContext) Method.() -> Boolean) =
    ReadOnlyProperty<BytecodePatchContext, Method?> { firstMethodOrNull(predicate) }

fun gettingFirstMethod(predicate: context(MatchContext) Method.() -> Boolean) =
    requireNotNull(gettingFirstMethodOrNull(predicate))

fun gettingFirstMethodMutableOrNull(predicate: context(MatchContext) Method.() -> Boolean) =
    ReadOnlyProperty<BytecodePatchContext, Method?> { firstMethodMutableOrNull(predicate) }

fun gettingFirstMethodMutable(predicate: context(MatchContext) Method.() -> Boolean) =
    requireNotNull(gettingFirstMethodMutableOrNull(predicate))

fun gettingFirstMethodOrNull(
    vararg strings: String,
    predicate: context(MatchContext) Method.() -> Boolean = { true },
) = ReadOnlyProperty<BytecodePatchContext, Method?> { firstMethodOrNull(*strings, predicate = predicate) }

fun gettingFirstMethod(
    vararg strings: String,
    predicate: context(MatchContext) Method.() -> Boolean = { true },
) = requireNotNull(gettingFirstMethodOrNull(*strings, predicate = predicate))

fun gettingFirstMethodMutableOrNull(
    vararg strings: String,
    predicate: context(MatchContext) Method.() -> Boolean = { true },
) = ReadOnlyProperty<BytecodePatchContext, Method?> { firstMethodMutableOrNull(*strings, predicate = predicate) }

fun gettingFirstMethodMutable(
    vararg strings: String,
    predicate: context(MatchContext) Method.() -> Boolean = { true },
) = requireNotNull(gettingFirstMethodMutableOrNull(*strings, predicate = predicate))

fun <T> indexedMatcher() = IndexedMatcher<T>()

// Add lambda to emit instructions if matched (or matched arg)
fun <T> indexedMatcher(build: IndexedMatcher<T>.() -> Unit) =
    IndexedMatcher<T>().apply(build)

fun <T> Iterable<T>.matchIndexed(build: IndexedMatcher<T>.() -> Unit) =
    indexedMatcher(build)(this)

context(_: MatchContext)
operator fun <T> IndexedMatcher<T>.invoke(iterable: Iterable<T>, key: String, builder: IndexedMatcher<T>.() -> Unit) =
    remember(key) { IndexedMatcher<T>().apply(builder) }(iterable)

context(_: MatchContext)
fun <T> Iterable<T>.matchIndexed(key: String, build: IndexedMatcher<T>.() -> Unit) =
    indexedMatcher<T>()(this, key, build)

abstract class Matcher<T, U> : MutableList<U> by mutableListOf() {
    var matchIndex = -1
        protected set

    abstract operator fun invoke(haystack: Iterable<T>): Boolean

    class MatchContext internal constructor() : MutableMap<String, Any> by mutableMapOf() {
    }
}

context(context: MatchContext)
inline fun <reified V : Any> remember(key: String, defaultValue: () -> V) =
    context[key] as? V ?: defaultValue().also { context[key] = it }

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

fun BytecodePatchContext.matchers() {
    val customMatcher = object : Matcher<Instruction, Instruction.() -> Boolean>() {
        override fun invoke(haystack: Iterable<Instruction>) = true
    }.apply { add { true } }

    // Probably gonna make this ctor internal.
    IndexedMatcher<Instruction>().apply { add { true } }
    // Since there is a function for it.
    val m = indexedMatcher<Instruction>()
    m.apply { add { true } }

    // You can directly use the extension function to match.
    listOf<Instruction>().matchIndexed { add { true } }

    // Inside a match context extension functions are cacheable:
    firstMethod { instructions.matchIndexed("key") { add { true } } }

    // Or create a matcher outside a context and use it in a MatchContext as follows:
    val match = indexedMatcher<Instruction>()
    firstMethod { match(instructions, "anotherKey") { first { opcode(Opcode.RETURN_VOID) } } }
    match.indices // so that you can access the matched indices later.
}

fun BytecodePatchContext.a() {
    val matcher = indexedMatcher<Instruction>()

    firstMethodMutableOrNull("string to find in lookupmap") {
        returnType == "L" && matcher(instructions, "key") {
            first {
                // The first instruction is a field reference to a field in the class of the method being matched.
                // We cache the field name using remember to avoid redundant lookups.
                fieldReference!!.name == remember("fieldName") {
                    firstClassDef(definingClass).fields.first().name
                }
            }
            after { fieldReference("fieldName") }
            after {
                opcode(Opcode.NEW_INSTANCE) && methodReference { toString() == "Lcom/example/MyClass;" }
            }
            // Followed by 2 to 4 string instructions starting with "test".
            after(atLeast = 1, atMost = 2) { string { startsWith("test") } }
        } && parameterTypes.matchIndexed("params") {
            // Fully dynamic environment to customize depending on your needs.
            operator fun String.plus(other: String) {
                after { this == this@plus }
                after { this == other }
            }

            "L" + "I" + "Z" // Matches parameter types "L", "I", "Z" in order.
        }
    }

    firstMethodMutable {
        accessFlags(AccessFlags.PUBLIC, AccessFlags.FINAL) && instructions.matchIndexed("anotherKey") {
            first { opcode(Opcode.RETURN_VOID) }
        }
    }
}
