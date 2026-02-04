@file:Suppress("unused")

package app.revanced.patcher

import app.revanced.com.android.tools.smali.dexlib2.mutable.MutableMethod
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

typealias ClassDefPredicate =
    ClassDef.() -> Boolean

typealias MethodPredicate =
    Method.() -> Boolean

typealias BytecodePatchContextMethodPredicate = context(BytecodePatchContext)
Method.() -> Boolean

typealias BytecodePatchContextClassDefPredicate = context(BytecodePatchContext)
ClassDef.() -> Boolean

private fun <R, T> cachedReadOnlyProperty(block: R.(KProperty<*>) -> T) =
    object : ReadOnlyProperty<R, T> {
        private val cache = HashMap<R, T>(1)

        override fun getValue(
            thisRef: R,
            property: KProperty<*>,
        ) = if (thisRef in cache) {
            cache.getValue(thisRef)
        } else {
            cache.getOrPut(thisRef) { thisRef.block(property) }
        }
    }

@JvmName("bytecodePatchContextCachedReadOnlyProperty")
private fun <T> cachedReadOnlyProperty(block: BytecodePatchContext.(KProperty<*>) -> T) =
    cachedReadOnlyProperty<BytecodePatchContext, T>(block)

class MutablePredicateList<T> internal constructor() : MutableList<Predicate<T>> by mutableListOf()

@JvmName("firstImmutableMethodOrNullInMethods")
fun Iterable<Method>.firstImmutableMethodOrNull(methodReference: MethodReference) =
    firstOrNull { MethodUtil.methodSignaturesMatch(methodReference, it) }

@JvmName("firstImmutableMethodInMethods")
fun Iterable<Method>.firstImmutableMethod(methodReference: MethodReference) = requireNotNull(firstImmutableMethodOrNull(methodReference))

@JvmName("firstMethodOrNullInMethods")
context(context: BytecodePatchContext)
fun Iterable<Method>.firstMethodOrNull(methodReference: MethodReference) =
    firstImmutableMethodOrNull(methodReference)?.let { context.firstMethod(it) }

@JvmName("firstMethodInMethods")
context(_: BytecodePatchContext)
fun Iterable<Method>.firstMethod(methodReference: MethodReference) = requireNotNull(firstMethodOrNull(methodReference))

@JvmName("firstImmutableMethodOrNullInMethods")
fun Iterable<Method>.firstImmutableMethodOrNull(
    vararg strings: String,
    predicate: MethodPredicate = { true },
) = if (strings.isEmpty()) {
    firstOrNull { it.predicate() }
} else {
    first { method ->
        val instructions = method.instructionsOrNull ?: return@first false

        // TODO: Check potential to optimize (Set or not).
        //  Maybe even use context maps, but the methods may not be present in the context yet.
        val methodStrings = instructions.asSequence().mapNotNull { it.string }.toSet()

        if (strings.any { it !in methodStrings }) return@first false

        method.predicate()
    }
}

@JvmName("firstImmutableMethodInMethods")
fun Iterable<Method>.firstImmutableMethod(
    vararg strings: String,
    predicate: MethodPredicate = { true },
) = requireNotNull(firstImmutableMethodOrNull(strings = strings, predicate))

@JvmName("firstMethodOrNullInMethods")
context(context: BytecodePatchContext)
fun Iterable<Method>.firstMethodOrNull(
    vararg strings: String,
    predicate: MethodPredicate = { true },
) = firstImmutableMethodOrNull(
    strings = strings,
    predicate,
)?.let { context.firstMethod(it) }

@JvmName("firstMethodInMethods")
context(_: BytecodePatchContext)
fun Iterable<Method>.firstMethod(
    vararg strings: String,
    predicate: MethodPredicate = { true },
) = requireNotNull(firstMethodOrNull(strings = strings, predicate))

@JvmName("firstImmutableMethodOrNullInClassDefs")
fun Iterable<ClassDef>.firstImmutableMethodOrNull(methodReference: MethodReference) =
    asSequence().flatMap { it.methods.asSequence() }.asIterable().firstImmutableMethodOrNull(methodReference)

@JvmName("firstImmutableMethodInClassDefs")
fun Iterable<ClassDef>.firstImmutableMethod(methodReference: MethodReference) = requireNotNull(firstImmutableMethodOrNull(methodReference))

@JvmName("firstMethodOrNullInClassDefs")
context(_: BytecodePatchContext)
fun Iterable<ClassDef>.firstMethodOrNull(methodReference: MethodReference) =
    asSequence().flatMap { it.methods.asSequence() }.asIterable().firstMethodOrNull(methodReference)

@JvmName("firstMethodInClassDefs")
context(_: BytecodePatchContext)
fun Iterable<ClassDef>.firstMethod(methodReference: MethodReference) = requireNotNull(firstMethodOrNull(methodReference))

@JvmName("firstImmutableMethodOrNullInClassDefs")
fun Iterable<ClassDef>.firstImmutableMethodOrNull(predicate: MethodPredicate = { true }) =
    asSequence().flatMap { it.methods.asSequence() }.asIterable().firstImmutableMethodOrNull(strings = emptyArray(), predicate)

@JvmName("firstImmutableMethodInClassDefs")
fun Iterable<ClassDef>.firstImmutableMethod(predicate: MethodPredicate = { true }) = requireNotNull(firstImmutableMethodOrNull(predicate))

@JvmName("firstImmutableMethodOrNullInClassDefs")
fun Iterable<ClassDef>.firstImmutableMethodOrNull(
    vararg strings: String,
    predicate: MethodPredicate = { true },
) = asSequence().flatMap { it.methods.asSequence() }.asIterable().firstImmutableMethodOrNull(strings = strings, predicate)

@JvmName("firstImmutableMethodInClassDefs")
fun Iterable<ClassDef>.firstImmutableMethod(
    vararg strings: String,
    predicate: MethodPredicate = { true },
) = requireNotNull(firstImmutableMethodOrNull(strings = strings, predicate))

@JvmName("firstMethodOrNullInClassDefs")
context(context: BytecodePatchContext)
fun Iterable<ClassDef>.firstMethodOrNull(
    vararg strings: String,
    predicate: MethodPredicate = { true },
) = firstImmutableMethodOrNull(
    strings = strings,
    predicate,
)?.let { context.firstMethod(it) }

@JvmName("firstMethodInClassDefs")
context(_: BytecodePatchContext)
fun Iterable<ClassDef>.firstMethod(
    vararg strings: String,
    predicate: MethodPredicate = { true },
) = requireNotNull(firstMethodOrNull(strings = strings, predicate))

@JvmName("firstImmutableMethodOrNullInClassDef")
fun ClassDef.firstImmutableMethodOrNull(methodReference: MethodReference) = methods.firstImmutableMethodOrNull(methodReference)

@JvmName("firstImmutableMethodInClassDef")
fun ClassDef.firstImmutableMethod(methodReference: MethodReference) = requireNotNull(firstImmutableMethodOrNull(methodReference))

@JvmName("firstMethodOrNullInClassDef")
context(_: BytecodePatchContext)
fun ClassDef.firstMethodOrNull(methodReference: MethodReference) = methods.firstMethodOrNull(methodReference)

@JvmName("firstMethodInClassDef")
context(_: BytecodePatchContext)
fun ClassDef.firstMethod(methodReference: MethodReference) = requireNotNull(firstMethodOrNull(methodReference))

@JvmName("firstImmutableMethodOrNullInClassDef")
fun ClassDef.firstImmutableMethodOrNull(
    vararg strings: String,
    predicate: MethodPredicate = { true },
) = methods.firstImmutableMethodOrNull(strings = strings, predicate)

@JvmName("firstImmutableMethodInClassDef")
fun ClassDef.firstImmutableMethod(
    vararg strings: String,
    predicate: MethodPredicate = { true },
) = requireNotNull(firstImmutableMethodOrNull(strings = strings, predicate))

@JvmName("firstMethodOrNullInClassDef")
context(_: BytecodePatchContext)
fun ClassDef.firstMethodOrNull(
    vararg strings: String,
    predicate: MethodPredicate = { true },
) = methods.firstMethodOrNull(strings = strings, predicate)

@JvmName("firstMethodInClassDef")
context(_: BytecodePatchContext)
fun ClassDef.firstMethod(
    vararg strings: String,
    predicate: MethodPredicate = { true },
) = requireNotNull(firstMethodOrNull(strings = strings, predicate))

@JvmName("firstImmutableClassDefOrNullInClassDefs")
fun Iterable<ClassDef>.firstImmutableClassDefOrNull(
    type: String? = null,
    predicate: ClassDefPredicate = { true },
) = if (type == null) {
    firstOrNull { it.predicate() }
} else {
    firstOrNull { it.type == type && it.predicate() }
}

@JvmName("firstImmutableClassDefInClassDefs")
fun Iterable<ClassDef>.firstImmutableClassDef(
    type: String? = null,
    predicate: ClassDefPredicate = { true },
) = requireNotNull(firstImmutableClassDefOrNull(type, predicate))

@JvmName("firstClassDefOrNullInClassDefs")
context(context: BytecodePatchContext)
fun Iterable<ClassDef>.firstClassDefOrNull(
    type: String? = null,
    predicate: ClassDefPredicate = { true },
) = if (type == null) {
    firstImmutableClassDefOrNull(type, predicate)
} else {
    context.classDefs[type].takeIf { it?.predicate() == true }
}?.let { context.classDefs.getOrReplaceMutable(it) }

@JvmName("firstClassDefInClassDefs")
context(_: BytecodePatchContext)
fun Iterable<ClassDef>.firstClassDef(
    type: String? = null,
    predicate: ClassDefPredicate = { true },
) = requireNotNull(firstClassDefOrNull(type, predicate))

fun BytecodePatchContext.firstImmutableMethodOrNull(methodReference: MethodReference) =
    firstImmutableClassDefOrNull(methodReference.definingClass)?.methods?.firstImmutableMethodOrNull(methodReference)

fun BytecodePatchContext.firstImmutableMethod(method: MethodReference) = requireNotNull(firstImmutableMethodOrNull(method))

fun BytecodePatchContext.firstMethodOrNull(methodReference: MethodReference): MutableMethod? =
    firstClassDefOrNull(methodReference.definingClass)?.methods?.first {
        MethodUtil.methodSignaturesMatch(
            methodReference,
            it,
        )
    }

fun BytecodePatchContext.firstMethod(method: MethodReference) = requireNotNull(firstMethodOrNull(method))

fun BytecodePatchContext.firstImmutableMethodOrNull(
    vararg strings: String,
    predicate: MethodPredicate = { true },
): Method? {
    if (strings.isEmpty()) return classDefs.firstImmutableMethodOrNull(predicate)

    // TODO: Get rid of duplicates, but this isn't needed for functionality. Perhaps worse performance-wise?
    val strings = strings.toSet()

    val methodsWithStrings = strings.mapNotNull { classDefs.methodsByString[it] }
    if (methodsWithStrings.size != strings.size) return null

    return methodsWithStrings.minBy { it.size }.firstOrNull { method ->
        val containsAllOtherStrings = methodsWithStrings.all { method in it }
        containsAllOtherStrings && method.predicate()
    }
}

fun BytecodePatchContext.firstImmutableMethod(
    vararg strings: String,
    predicate: MethodPredicate = { true },
) = requireNotNull(firstImmutableMethodOrNull(strings = strings, predicate))

fun BytecodePatchContext.firstMethodOrNull(
    vararg strings: String,
    predicate: MethodPredicate = { true },
) = firstImmutableMethodOrNull(strings = strings, predicate)?.let { method ->
    firstMethodOrNull(method)
}

fun BytecodePatchContext.firstMethod(
    vararg strings: String,
    predicate: MethodPredicate = { true },
) = requireNotNull(firstMethodOrNull(strings = strings, predicate))

fun gettingFirstImmutableMethodOrNull(method: MethodReference) = cachedReadOnlyProperty { firstImmutableMethodOrNull(method) }

fun gettingFirstImmutableMethod(method: MethodReference) = cachedReadOnlyProperty { firstImmutableMethod(method) }

fun gettingFirstMethodOrNull(method: MethodReference) = cachedReadOnlyProperty { firstMethodOrNull(method) }

fun gettingFirstMethod(method: MethodReference) = cachedReadOnlyProperty { firstMethod(method) }

fun gettingFirstImmutableMethodOrNull(
    vararg strings: String,
    predicate: BytecodePatchContextMethodPredicate = { true },
) = cachedReadOnlyProperty { firstImmutableMethodOrNull(strings = strings) { predicate() } }

fun gettingFirstImmutableMethod(
    vararg strings: String,
    predicate: BytecodePatchContextMethodPredicate = { true },
) = cachedReadOnlyProperty { firstImmutableMethod(strings = strings) { predicate() } }

fun gettingFirstMethodOrNull(
    vararg strings: String,
    predicate: BytecodePatchContextMethodPredicate = { true },
) = cachedReadOnlyProperty { firstMethodOrNull(strings = strings) { predicate() } }

fun gettingFirstMethod(
    vararg strings: String,
    predicate: BytecodePatchContextMethodPredicate = { true },
) = cachedReadOnlyProperty { firstMethod(strings = strings) { predicate() } }

fun BytecodePatchContext.firstImmutableClassDefOrNull(
    type: String? = null,
    predicate: ClassDefPredicate = { true },
) = if (type == null) {
    classDefs.firstImmutableClassDefOrNull(type, predicate)
} else {
    classDefs[type]?.takeIf { it.predicate() }
}

fun BytecodePatchContext.firstImmutableClassDef(
    type: String? = null,
    predicate: ClassDefPredicate = { true },
) = requireNotNull(firstImmutableClassDefOrNull(type, predicate))

fun BytecodePatchContext.firstClassDefOrNull(
    type: String? = null,
    predicate: ClassDefPredicate = { true },
) = firstImmutableClassDefOrNull(type, predicate)?.let { classDefs.getOrReplaceMutable(it) }

fun BytecodePatchContext.firstClassDef(
    type: String? = null,
    predicate: ClassDefPredicate = { true },
) = requireNotNull(firstClassDefOrNull(type, predicate))

fun gettingFirstImmutableClassDefOrNull(
    type: String? = null,
    predicate: BytecodePatchContextClassDefPredicate = { true },
) = cachedReadOnlyProperty { firstImmutableClassDefOrNull(type) { predicate() } }

fun gettingFirstImmutableClassDef(
    type: String? = null,
    predicate: BytecodePatchContextClassDefPredicate = { true },
) = cachedReadOnlyProperty { firstImmutableClassDef(type) { predicate() } }

fun gettingFirstClassDefOrNull(
    type: String? = null,
    predicate: BytecodePatchContextClassDefPredicate = { true },
) = cachedReadOnlyProperty { firstClassDefOrNull(type) { predicate() } }

fun gettingFirstClassDef(
    type: String? = null,
    predicate: BytecodePatchContextClassDefPredicate = { true },
) = cachedReadOnlyProperty { firstClassDef(type) { predicate() } }

private fun <T, R> buildPredicate(
    build: MutablePredicateList<T>.() -> Unit = { },
    block: (predicate: Predicate<T>) -> R,
) = block(MutablePredicateList<T>().apply { build() }::all)

private fun <T> buildPredicate(
    strings: Array<out String>,
    build: context(MutableList<String>) MutablePredicateList<Method>.() -> Unit = { },
    block: (strings: Array<String>, predicate: MethodPredicate) -> T,
) = with(mutableListOf(elements = strings)) {
    buildPredicate({ build() }) { predicate -> block(toTypedArray(), predicate) }
}

@JvmName("firstImmutableMethodDeclarativelyOrNullInMethods")
fun Iterable<Method>.firstImmutableMethodDeclarativelyOrNull(
    vararg strings: String,
    build: context(MutableList<String>) MutablePredicateList<Method>.() -> Unit = { },
) = buildPredicate(strings, build, ::firstImmutableMethodOrNull)

@JvmName("firstImmutableMethodDeclarativelyInMethods")
fun Iterable<Method>.firstImmutableMethodDeclaratively(
    vararg strings: String,
    build: context(MutableList<String>) MutablePredicateList<Method>.() -> Unit = { },
) = requireNotNull(firstImmutableMethodDeclarativelyOrNull(strings = strings, build))

@JvmName("firstMethodDeclarativelyOrNullInMethods")
context(_: BytecodePatchContext)
fun Iterable<Method>.firstMethodDeclarativelyOrNull(
    vararg strings: String,
    build: context(MutableList<String>) MutablePredicateList<Method>.() -> Unit = { },
) = buildPredicate(strings, build) { strings, predicate -> firstMethodOrNull(strings = strings, predicate) }

@JvmName("firstMethodDeclarativelyInMethods")
context(_: BytecodePatchContext)
fun Iterable<Method>.firstMethodDeclaratively(
    vararg strings: String,
    build: context(MutableList<String>) MutablePredicateList<Method>.() -> Unit = { },
) = requireNotNull(firstMethodDeclarativelyOrNull(strings = strings, build))

@JvmName("firstImmutableMethodDeclarativelyOrNullInClassDefs")
fun Iterable<ClassDef>.firstImmutableMethodDeclarativelyOrNull(build: MutablePredicateList<Method>.() -> Unit) =
    buildPredicate(build, ::firstImmutableMethodOrNull)

@JvmName("firstImmutableMethodDeclarativelyInClassDefs")
fun Iterable<ClassDef>.firstImmutableMethodDeclaratively(build: MutablePredicateList<Method>.() -> Unit) =
    requireNotNull(firstImmutableMethodDeclarativelyOrNull(build))

@JvmName("firstImmutableMethodDeclarativelyOrNullInClassDefs")
fun Iterable<ClassDef>.firstImmutableMethodDeclarativelyOrNull(
    vararg strings: String,
    build: context(MutableList<String>) MutablePredicateList<Method>.() -> Unit = { },
) = buildPredicate(strings, build, ::firstImmutableMethodOrNull)

@JvmName("firstImmutableMethodDeclarativelyInClassDefs")
fun Iterable<ClassDef>.firstImmutableMethodDeclaratively(
    vararg strings: String,
    build: context(MutableList<String>) MutablePredicateList<Method>.() -> Unit = { },
) = requireNotNull(firstImmutableMethodDeclarativelyOrNull(strings = strings, build))

@JvmName("firstMethodDeclarativelyOrNullInClassDefs")
context(context: BytecodePatchContext)
fun Iterable<ClassDef>.firstMethodDeclarativelyOrNull(
    vararg strings: String,
    build: context(MutableList<String>) MutablePredicateList<Method>.() -> Unit = { },
) = buildPredicate(strings, build) { strings, predicate -> firstMethodOrNull(strings = strings, predicate) }

@JvmName("firstImmutableMethodDeclarativelyOrNullInClassDef")
fun ClassDef.firstImmutableMethodDeclarativelyOrNull(
    vararg strings: String,
    build: context(MutableList<String>) MutablePredicateList<Method>.() -> Unit = { },
) = methods.firstImmutableMethodDeclarativelyOrNull(strings = strings, build)

@JvmName("firstImmutableMethodDeclarativelyInClassDef")
fun ClassDef.firstImmutableMethodDeclaratively(
    vararg strings: String,
    build: context(MutableList<String>) MutablePredicateList<Method>.() -> Unit = { },
) = requireNotNull(firstImmutableMethodDeclarativelyOrNull(strings = strings, build))

@JvmName("firstMethodDeclarativelyOrNullInClassDef")
context(_: BytecodePatchContext)
fun ClassDef.firstMethodDeclarativelyOrNull(
    vararg strings: String,
    build: context(MutableList<String>) MutablePredicateList<Method>.() -> Unit = { },
) = methods.firstMethodDeclarativelyOrNull(strings = strings, build)

@JvmName("firstMethodDeclarativelyInClassDef")
context(_: BytecodePatchContext)
fun ClassDef.firstMethodDeclaratively(
    vararg strings: String,
    build: context(MutableList<String>) MutablePredicateList<Method>.() -> Unit = { },
) = requireNotNull(firstMethodDeclarativelyOrNull(strings = strings, build))

@JvmName("firstImmutableClassDefDeclarativelyOrNullInClassDefs")
fun Iterable<ClassDef>.firstImmutableClassDefDeclarativelyOrNull(
    type: String? = null,
    build: MutablePredicateList<ClassDef>.() -> Unit = { },
) = buildPredicate(build) { predicate -> firstImmutableClassDefOrNull(type, predicate) }

@JvmName("firstImmutableClassDefDeclarativelyInClassDefs")
fun Iterable<ClassDef>.firstImmutableClassDefDeclaratively(
    type: String? = null,
    build: MutablePredicateList<ClassDef>.() -> Unit = { },
) = requireNotNull(firstImmutableClassDefDeclarativelyOrNull(type, build))

@JvmName("firstClassDefDeclarativelyOrNullInClassDefs")
context(_: BytecodePatchContext)
fun Iterable<ClassDef>.firstClassDefDeclarativelyOrNull(
    type: String? = null,
    build: MutablePredicateList<ClassDef>.() -> Unit = { },
) = buildPredicate(build) { predicate ->
    firstClassDefOrNull(type, predicate)
}

@JvmName("firstClassDefDeclarativelyInClassDefs")
context(_: BytecodePatchContext)
fun Iterable<ClassDef>.firstClassDefDeclaratively(
    type: String? = null,
    build: MutablePredicateList<ClassDef>.() -> Unit = { },
) = requireNotNull(firstClassDefDeclarativelyOrNull(type, build))

fun BytecodePatchContext.firstImmutableMethodDeclarativelyOrNull(
    vararg strings: String,
    build: context(MutableList<String>) MutablePredicateList<Method>.() -> Unit = { },
) = buildPredicate(strings, build) { strings, predicate -> firstImmutableMethodOrNull(strings = strings, predicate) }

fun BytecodePatchContext.firstImmutableMethodDeclaratively(
    vararg strings: String,
    build: context(MutableList<String>) MutablePredicateList<Method>.() -> Unit = { },
) = requireNotNull(firstImmutableMethodDeclarativelyOrNull(strings = strings, build))

fun BytecodePatchContext.firstMethodDeclarativelyOrNull(
    vararg strings: String,
    build: context(MutableList<String>) MutablePredicateList<Method>.() -> Unit = { },
) = buildPredicate(strings, build) { strings, predicate -> firstMethodOrNull(strings = strings, predicate) }

fun BytecodePatchContext.firstMethodDeclaratively(
    vararg strings: String,
    build: context(MutableList<String>) MutablePredicateList<Method>.() -> Unit = { },
) = requireNotNull(firstMethodDeclarativelyOrNull(strings = strings, build))

fun gettingFirstImmutableMethodDeclarativelyOrNull(
    vararg strings: String,
    build: context(BytecodePatchContext, MutableList<String>) MutablePredicateList<Method>.() -> Unit = {},
) = cachedReadOnlyProperty { firstImmutableMethodDeclarativelyOrNull(strings = strings) { build() } }

fun gettingFirstImmutableMethodDeclaratively(
    vararg strings: String,
    build: context(BytecodePatchContext, MutableList<String>) MutablePredicateList<Method>.() -> Unit = {},
) = cachedReadOnlyProperty { firstImmutableMethodDeclaratively(strings = strings) { build() } }

fun gettingFirstMethodDeclarativelyOrNull(
    vararg strings: String,
    build: context(BytecodePatchContext, MutableList<String>) MutablePredicateList<Method>.() -> Unit = {},
) = cachedReadOnlyProperty { firstMethodDeclarativelyOrNull(strings = strings) { build() } }

fun gettingFirstMethodDeclaratively(
    vararg strings: String,
    build: context(BytecodePatchContext, MutableList<String>) MutablePredicateList<Method>.() -> Unit = {},
) = cachedReadOnlyProperty { firstMethodDeclaratively(strings = strings) { build() } }

fun BytecodePatchContext.firstImmutableClassDefDeclarativelyOrNull(
    type: String? = null,
    build: MutablePredicateList<ClassDef>.() -> Unit = { },
) = buildPredicate(build) { predicate -> firstImmutableClassDefOrNull(type, predicate) }

fun BytecodePatchContext.firstImmutableClassDefDeclaratively(
    type: String? = null,
    build: MutablePredicateList<ClassDef>.() -> Unit = { },
) = buildPredicate(build) { predicate -> firstImmutableClassDef(type, predicate) }

fun BytecodePatchContext.firstClassDefDeclarativelyOrNull(
    type: String? = null,
    build: MutablePredicateList<ClassDef>.() -> Unit = { },
) = buildPredicate(build) { predicate -> firstClassDefOrNull(type, predicate) }

fun BytecodePatchContext.firstClassDefDeclaratively(
    type: String? = null,
    build: MutablePredicateList<ClassDef>.() -> Unit = { },
) = buildPredicate(build) { predicate -> firstClassDef(type, predicate) }

fun gettingFirstImmutableClassDefDeclarativelyOrNull(
    type: String? = null,
    build: context(BytecodePatchContext) MutablePredicateList<ClassDef>.() -> Unit = { },
) = cachedReadOnlyProperty { firstImmutableClassDefDeclarativelyOrNull(type) { build() } }

fun gettingFirstImmutableClassDefDeclaratively(
    type: String? = null,
    build: context(BytecodePatchContext) MutablePredicateList<ClassDef>.() -> Unit = { },
) = cachedReadOnlyProperty { firstImmutableClassDefDeclaratively(type) { build() } }

fun gettingFirstClassDefDeclarativelyOrNull(
    type: String? = null,
    build: context(BytecodePatchContext) MutablePredicateList<ClassDef>.() -> Unit = { },
) = cachedReadOnlyProperty { firstClassDefDeclarativelyOrNull(type) { build() } }

fun gettingFirstClassDefDeclaratively(
    type: String? = null,
    build: context(BytecodePatchContext) MutablePredicateList<ClassDef>.() -> Unit = { },
) = cachedReadOnlyProperty { firstClassDefDeclaratively { build() } }

typealias IndexedMatcherPredicate<T> = T.(lastMatchedIndex: Int, currentIndex: Int, setNextIndex: (Int?) -> Unit) -> Boolean

fun <T> indexedMatcher(vararg items: IndexedMatcherPredicate<T>) =
    IndexedMatcher<T>().apply {
        items.forEach { +it }
    }

fun <T> indexedMatcher(build: Function<IndexedMatcher<T>>) = IndexedMatcher<T>().apply(build)

fun <T> Iterable<T>.matchIndexed(build: Function<IndexedMatcher<T>>) = indexedMatcher(build)(this)

fun <T> at(
    index: Int,
    predicate: IndexedMatcherPredicate<T>,
): IndexedMatcherPredicate<T> =
    { lastMatchedIndex, currentIndex, setNextIndex ->
        currentIndex == index && predicate(lastMatchedIndex, currentIndex, setNextIndex)
    }

fun <T> at(
    index: Int,
    predicate: Predicate<T>,
) = at<T>(index) { _, _, _ -> predicate() }

fun <T> after(
    range: IntRange = 1..1,
    predicate: IndexedMatcherPredicate<T>,
): IndexedMatcherPredicate<T> =
    predicate@{ lastMatchedIndex, currentIndex, setNextIndex ->
        val distance = currentIndex - lastMatchedIndex

        setNextIndex(
            when {
                distance < range.first -> lastMatchedIndex + range.first
                distance > range.last -> -1
                else -> return@predicate predicate(lastMatchedIndex, currentIndex, setNextIndex)
            },
        )

        false
    }

fun <T> after(
    range: IntRange = 1..1,
    predicate: Predicate<T>,
) = after<T>(range) { _, _, _ -> predicate() }

fun <T> after(predicate: IndexedMatcherPredicate<T>) =
    after<T>(1..1) { lastMatchedIndex, currentIndex, setNextIndex ->
        predicate(lastMatchedIndex, currentIndex, setNextIndex)
    }

fun <T> after(predicate: Predicate<T>) = after<T> { _, _, _ -> predicate() }

fun <T> afterAtLeast(
    steps: Int = 1,
    predicate: IndexedMatcherPredicate<T>,
) = after<T>(steps..steps) { lastMatchedIndex, currentIndex, setNextIndex ->
    predicate(lastMatchedIndex, currentIndex, setNextIndex)
}

fun <T> afterAtLeast(
    steps: Int = 1,
    predicate: Predicate<T>,
) = after<T>(steps..Int.MAX_VALUE) { _, _, _ -> predicate() }

fun <T> afterAtMost(
    steps: Int = 1,
    predicate: IndexedMatcherPredicate<T>,
) = after<T>(1..steps) { lastMatchedIndex, currentIndex, setNextIndex ->
    predicate(lastMatchedIndex, currentIndex, setNextIndex)
}

fun <T> afterAtMost(
    steps: Int = 1,
    predicate: Predicate<T>,
) = after<T>(1..steps) { _, _, _ -> predicate() }

fun <T> after(
    steps: Int = 1,
    predicate: IndexedMatcherPredicate<T>,
) = after<T>(steps..steps) { lastMatchedIndex, currentIndex, setNextIndex ->
    predicate(lastMatchedIndex, currentIndex, setNextIndex)
}

fun <T> after(
    steps: Int = 1,
    predicate: Predicate<T>,
) = after<T>(steps..steps) { _, _, _ -> predicate() }

fun <T> anyOf(vararg predicates: IndexedMatcherPredicate<T>): IndexedMatcherPredicate<T> =
    { currentIndex, lastMatchedIndex, setNextIndex ->
        predicates.any { predicate -> predicate(currentIndex, lastMatchedIndex, setNextIndex) }
    }

fun <T> allOf(vararg predicates: IndexedMatcherPredicate<T>): IndexedMatcherPredicate<T> =
    { currentIndex, lastMatchedIndex, setNextIndex ->
        predicates.all { predicate -> predicate(currentIndex, lastMatchedIndex, setNextIndex) }
    }

fun <T> noneOf(vararg predicates: IndexedMatcherPredicate<T>): IndexedMatcherPredicate<T> =
    { currentIndex, lastMatchedIndex, setNextIndex ->
        predicates.none { predicate -> predicate(currentIndex, lastMatchedIndex, setNextIndex) }
    }

fun <T> unorderedAllOf(vararg predicates: IndexedMatcherPredicate<T>): Array<IndexedMatcherPredicate<T>> {
    // Track which predicate index was used.
    val usedPredicateIndices = mutableListOf<Int>()
    var lastPatternIndex = -1

    return (0 until predicates.size)
        .map<Int, IndexedMatcherPredicate<T>> { patternIndex ->
            predicate@{ lastMatchedIndex, currentIndex, setNextIndex ->
                // Detect backtracking: if revisiting an earlier pattern position, truncate the list to that position.
                if (patternIndex <= lastPatternIndex) {
                    while (usedPredicateIndices.size > patternIndex) {
                        usedPredicateIndices.removeLast()
                    }
                }

                lastPatternIndex = patternIndex

                // Try each unused predicate.
                for (predicateIndex in predicates.indices) {
                    if (predicateIndex in usedPredicateIndices) continue

                    if (predicates[predicateIndex](lastMatchedIndex, currentIndex) { nextIndex ->
                            // Backtracking is not possible in an unordered matcher.
                            // If backtracking is requested, just set to max value to end searching.
                            if (nextIndex != -1) setNextIndex(Int.MAX_VALUE)
                        }
                    ) {
                        usedPredicateIndices += predicateIndex
                        return@predicate true
                    }
                }

                false
            }
        }.toTypedArray()
}

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
            val matchedIndex: Int,
        )

        val stack = ArrayDeque<Frame>()
        stack.add(
            Frame(
                patternIndex = 0,
                lastMatchedIndex = -1,
                previousFrame = null,
                nextHayIndex = 0,
                matchedIndex = -1,
            ),
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
                    matchedIndex = i,
                ).also {
                    if (it.patternIndex == size) {
                        indices +=
                            buildList(size) {
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

            frame.nextHayIndex =
                when (val nextIndex = nextIndex) {
                    null -> frame.nextHayIndex + 1

                    -1 -> 0

                    // Frame will be removed next loop.
                    else -> nextIndex
                }
        }

        return false
    }
}

// region Matcher

context(matcher: M)
operator fun <T, U, M : Matcher<T, U>> U.unaryPlus() = matcher.add(this)

abstract class Matcher<T, U> : MutableList<U> by mutableListOf() {
    var matchIndex = -1
        protected set

    abstract operator fun invoke(haystack: Iterable<T>): Boolean
}

// endregion Matcher

// region MutablePredicateList extensions

fun <T> MutablePredicateList<T>.allOf(block: MutablePredicateList<T>.() -> Unit) {
    val child = MutablePredicateList<T>().apply(block)
    add { child.all { it() } }
}

fun <T> MutablePredicateList<T>.anyOf(block: MutablePredicateList<T>.() -> Unit) {
    val child = MutablePredicateList<T>().apply(block)
    add { child.any { it() } }
}

fun <T> MutablePredicateList<T>.noneOf(block: MutablePredicateList<T>.() -> Unit) {
    val child = MutablePredicateList<T>().apply(block)
    add { child.none { it() } }
}

fun <T> MutablePredicateList<T>.predicate(block: Predicate<T>) {
    add(block)
}

fun <T> MutablePredicateList<T>.all(target: T): Boolean = all { target.it() }

fun <T> MutablePredicateList<T>.any(target: T): Boolean = any { target.it() }

fun MutablePredicateList<Method>.accessFlags(vararg flags: AccessFlags) = predicate { accessFlags(flags = flags) }

fun MutablePredicateList<Method>.returnType(predicate: Predicate<String>) = predicate { returnType.predicate() }

fun MutablePredicateList<Method>.returnType(
    returnType: String,
    compare: String.(String) -> Boolean = returnType.typeComparer(),
) = predicate { this.returnType.compare(returnType) }

fun MutablePredicateList<Method>.name(predicate: Predicate<String>) = predicate { name.predicate() }

fun MutablePredicateList<Method>.name(
    name: String,
    compare: String.(String) -> Boolean = String::equals,
) = predicate { this.name.compare(name) }

fun MutablePredicateList<Method>.definingClass(predicate: Predicate<String>) = predicate { definingClass.predicate() }

fun MutablePredicateList<Method>.definingClass(
    definingClass: String,
    compare: String.(String) -> Boolean = definingClass.typeComparer()
) = predicate { this.definingClass.compare(definingClass) }

fun MutablePredicateList<Method>.parameterTypes(vararg parameterTypePrefixes: String) =
    predicate {
        parameterTypes.size == parameterTypePrefixes.size &&
            parameterTypes.zip(parameterTypePrefixes).all { (a, b) -> a.startsWith(b) }
    }

fun MutablePredicateList<Method>.instructions(build: Function<IndexedMatcher<Instruction>>) {
    val match = indexedMatcher(build)

    predicate { implementation { match(instructions) } }
}

fun MutablePredicateList<Method>.instructions(vararg predicates: IndexedMatcherPredicate<Instruction>) =
    instructions {
        predicates.forEach { +it }
    }

context(matchers: MutableList<IndexedMatcher<Instruction>>)
fun MutablePredicateList<Method>.instructions(build: Function<IndexedMatcher<Instruction>>) {
    val match = indexedMatcher(build).also(matchers::add)

    predicate { implementation { match(instructions) } }
}

context(matchers: MutableList<IndexedMatcher<Instruction>>)
fun MutablePredicateList<Method>.instructions(vararg predicates: IndexedMatcherPredicate<Instruction>) =
    instructions { predicates.forEach { +it } }

fun MutablePredicateList<Method>.instructions(vararg predicates: Predicate<Instruction>) =
    instructions { predicates.forEach { add { _, _, _ -> it() } } }

fun MutablePredicateList<Method>.custom(block: Predicate<Method>) {
    predicate { block() }
}

fun MutablePredicateList<Method>.opcodes(vararg opcodes: Opcode) =
    instructions {
        +opcodes.first()()
        opcodes.drop(1).forEach { +after(it()) }
    }

context(matchers: MutableList<IndexedMatcher<Instruction>>)
fun MutablePredicateList<Method>.opcodes(vararg opcodes: Opcode) =
    instructions {
        +opcodes.first()()
        opcodes.drop(1).forEach { +after(it()) }
    }

private fun Array<out String>.toUnorderedStringPredicates() = unorderedAllOf(predicates = map { string(it) }.toTypedArray())

fun MutablePredicateList<Method>.strings(vararg strings: String) = instructions(predicates = strings.toUnorderedStringPredicates())

context(matchers: MutableList<IndexedMatcher<Instruction>>)
fun MutablePredicateList<Method>.strings(vararg strings: String) = instructions(predicates = strings.toUnorderedStringPredicates())

inline fun <reified T : Instruction> `is`(crossinline predicate: Predicate<T> = { true }): IndexedMatcherPredicate<Instruction> =
    { _, _, _ -> (this as? T)?.predicate() == true }

fun instruction(predicate: Predicate<Instruction> = { true }): IndexedMatcherPredicate<Instruction> = { _, _, _ -> predicate() }

fun registers(predicate: Predicate<IntArray> = { true }): IndexedMatcherPredicate<Instruction> =
    { _, _, _ ->
        when (this) {
            is RegisterRangeInstruction -> {
                IntArray(registerCount) { startRegister + it }.predicate()
            }

            is FiveRegisterInstruction -> {
                intArrayOf(
                    registerC,
                    registerD,
                    registerE,
                    registerF,
                    registerG,
                ).predicate()
            }

            is ThreeRegisterInstruction -> {
                intArrayOf(registerA, registerB, registerC).predicate()
            }

            is TwoRegisterInstruction -> {
                intArrayOf(registerA, registerB).predicate()
            }

            is OneRegisterInstruction -> {
                intArrayOf(registerA).predicate()
            }

            else -> {
                false
            }
        }
    }

fun registers(
    vararg registers: Int,
    compare: IntArray.(registers: IntArray) -> Boolean = { registers ->
        this.size >= registers.size && registers.indices.all { this[it] == registers[it] }
    },
) = registers({ compare(registers) })

fun literal(predicate: Predicate<Long> = { true }): IndexedMatcherPredicate<Instruction> = { _, _, _ -> wideLiteral?.predicate() == true }

fun literal(
    literal: Long,
    compare: Long.(Long) -> Boolean = Long::equals,
) = literal { compare(literal) }

operator fun Long.invoke(compare: Long.(Long) -> Boolean = Long::equals) = literal(this, compare)

inline fun <reified T : Reference> reference(crossinline predicate: Predicate<T> = { true }): IndexedMatcherPredicate<Instruction> =
    { _, _, _ -> (reference as? T)?.predicate() == true }

fun reference(
    reference: String,
    compare: String.(String) -> Boolean = String::contains,
): IndexedMatcherPredicate<Instruction> = { _, _, _ -> this.reference?.toString()?.compare(reference) == true }

fun field(predicate: Predicate<FieldReference> = { true }): IndexedMatcherPredicate<Instruction> =
    { _, _, _ ->
        fieldReference?.predicate() == true
    }

fun field(
    name: String,
    compare: String.(String) -> Boolean = String::equals,
) = field { this.name.compare(name) }

fun type(predicate: Predicate<String> = { true }): IndexedMatcherPredicate<Instruction> = { _, _, _ -> type?.predicate() == true }

fun type(
    type: String,
    compare: String.(type: String) -> Boolean = type.typeComparer(),
) = type { compare(type) }

fun method(predicate: Predicate<MethodReference> = { true }): IndexedMatcherPredicate<Instruction> =
    { _, _, _ ->
        methodReference?.predicate() == true
    }

fun method(
    name: String,
    compare: String.(String) -> Boolean = String::equals,
) = method { this.name.compare(name) }

fun string(predicate: Predicate<String> = { true }): IndexedMatcherPredicate<Instruction> =
    { _, _, _ ->
        string?.predicate() == true
    }

context(stringsList: MutableList<String>)
fun string(
    string: String,
    compare: String.(String) -> Boolean = String::equals,
): IndexedMatcherPredicate<Instruction> {
    if (compare == String::equals) stringsList += string

    return string { compare(string) }
}

fun string(
    string: String,
    compare: String.(String) -> Boolean = String::equals,
) = string { compare(string) }

operator fun String.invoke(compare: String.(String) -> Boolean = String::equals) = string(this, compare)

context(stringsList: MutableList<String>)
operator fun String.invoke(compare: String.(String) -> Boolean = String::equals) = string(this, compare)

operator fun Opcode.invoke(): IndexedMatcherPredicate<Instruction> = { _, _, _ -> opcode == this@invoke }

// Returns a comparer function based on the format of a type descriptor string.
private fun String.typeComparer(): String.(String)-> Boolean = when {
    startsWith("L") && endsWith(";") -> String::equals
    startsWith("L") -> String::startsWith
    endsWith(";") -> String::endsWith
    else -> String::contains
}

typealias DeclarativePredicateCompositeBuilder =
    context(
        MutableList<IndexedMatcher<Instruction>>,
        MutableList<String>
    )
    MutablePredicateList<Method>.() -> Unit

typealias BytecodePatchContextDeclarativePredicateCompositeBuilder =
    context(
        BytecodePatchContext,
        MutableList<IndexedMatcher<Instruction>>,
        MutableList<String>
    )
    MutablePredicateList<Method>.() -> Unit

fun BytecodePatchContext.firstMethodComposite(
    vararg strings: String,
    build: BytecodePatchContextDeclarativePredicateCompositeBuilder = {},
) = CompositeMatch(strings, { build() }) { strings, build -> firstImmutableMethodOrNull(strings = strings, build) }

fun Iterable<ClassDef>.firstMethodComposite(
    vararg strings: String,
    build: DeclarativePredicateCompositeBuilder = {},
) = CompositeMatch(strings, build) { strings, build -> firstImmutableMethodOrNull(strings = strings, build) }

fun ClassDef.firstMethodComposite(
    vararg strings: String,
    build: DeclarativePredicateCompositeBuilder = {},
) = CompositeMatch(strings, build) { strings, build -> firstImmutableMethodOrNull(strings = strings, build) }

fun composingFirstMethod(
    vararg strings: String,
    build: BytecodePatchContextDeclarativePredicateCompositeBuilder = {},
) = cachedReadOnlyProperty { firstMethodComposite(strings = strings, build) }

// Such objects can be made for the getting functions as well, if desired.

object ClassDefComposing {
    fun composingFirstMethod(
        vararg strings: String,
        build: DeclarativePredicateCompositeBuilder = {},
    ) = cachedReadOnlyProperty<ClassDef, CompositeMatch> { firstMethodComposite(strings = strings, build) }
}

object IterableClassDefComposing {
    fun composingFirstMethod(
        vararg strings: String,
        build: DeclarativePredicateCompositeBuilder = {},
    ) = cachedReadOnlyProperty<Iterable<ClassDef>, CompositeMatch> { firstMethodComposite(strings = strings, build) }
}

fun <T> composingMethod(
    getMethod: T.(strings: Array<out String>, predicate: Predicate<Method>) -> Method?,
    build: DeclarativePredicateCompositeBuilder = {},
) = cachedReadOnlyProperty<T, CompositeMatch> {
    CompositeMatch(emptyArray(), build) { strings, predicate -> getMethod(strings, predicate) }
}

// TODO: Rename this to Match after old Match is removed.
open class CompositeMatch(
    private val strings: Array<out String>,
    private val build: DeclarativePredicateCompositeBuilder,
    private val getImmutableMethodOrNull: (strings: Array<out String>, predicate: Predicate<Method>) -> Method?,
) {
    private val matchers = mutableListOf<IndexedMatcher<Instruction>>()

    val indices: List<List<Int>> by lazy {
        immutableMethod // Ensure matched.
        matchers.map { it.indices }
    }
    val immutableMethodOrNull by lazy {
        val strings = strings.toMutableList()

        buildPredicate({ context(matchers, strings) { build() } }) { predicate ->
            getImmutableMethodOrNull(strings.toTypedArray(), predicate)
        }
    }

    val immutableMethod by lazy { requireNotNull(immutableMethodOrNull) }

    private val BytecodePatchContext._methodOrNull by cachedReadOnlyProperty {
        firstMethodOrNull(immutableMethodOrNull ?: return@cachedReadOnlyProperty null)
    }

    context(context: BytecodePatchContext)
    val methodOrNull get() = context._methodOrNull

    context(_: BytecodePatchContext)
    val method get() = requireNotNull(methodOrNull)

    private val BytecodePatchContext._immutableClassDefOrNull by cachedReadOnlyProperty {
        val type = immutableMethodOrNull?.definingClass ?: return@cachedReadOnlyProperty null
        firstImmutableClassDefOrNull(type)
    }

    context(context: BytecodePatchContext)
    val immutableClassDefOrNull get() = context._immutableClassDefOrNull

    context(context: BytecodePatchContext)
    val immutableClassDef get() = requireNotNull(immutableClassDefOrNull)

    private val BytecodePatchContext._classDefOrNull by cachedReadOnlyProperty {
        val type = immutableMethodOrNull?.definingClass ?: return@cachedReadOnlyProperty null
        firstClassDefOrNull(type)
    }

    context(context: BytecodePatchContext)
    val classDefOrNull get() = context._classDefOrNull

    context(_: BytecodePatchContext)
    val classDef get() = requireNotNull(classDefOrNull)

    // This is opinionated, but aimed to assist expected usage. Could be generic and open to change if needed.

    context(_: BytecodePatchContext)
    operator fun component1() = method

    context(_: BytecodePatchContext)
    operator fun component2() = indices

    context(_: BytecodePatchContext)
    operator fun component3() = immutableClassDef

    operator fun get(index: Int) = indices.first().let { first -> first[index.mod(first.size)] }

    operator fun get(
        matcherIndex: Int,
        index: Int,
    ) = indices[matcherIndex.mod(indices[0].size)].let { indices -> indices[index.mod(indices.size)] }
}

context(context: BytecodePatchContext)
val Method.immutableClassDefOrNull get() = context.firstImmutableClassDefOrNull(definingClass)

context(_: BytecodePatchContext)
val Method.immutableClassDef get() = requireNotNull(immutableClassDefOrNull)

context(context: BytecodePatchContext)
val Method.classDefOrNull get() = context.firstClassDefOrNull(definingClass)

context(_: BytecodePatchContext)
val Method.classDef get() = requireNotNull(classDefOrNull)
