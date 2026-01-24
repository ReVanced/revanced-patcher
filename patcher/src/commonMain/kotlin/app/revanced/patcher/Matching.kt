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

typealias ClassDefPredicate = context(PredicateContext)
ClassDef.() -> Boolean
typealias MethodPredicate = context(PredicateContext)
Method.() -> Boolean
typealias BytecodePatchContextMethodPredicate = context(BytecodePatchContext, PredicateContext)
Method.() -> Boolean
typealias BytecodePatchContextClassDefPredicate = context(BytecodePatchContext, PredicateContext)
ClassDef.() -> Boolean

inline fun <reified V> PredicateContext.remember(
    key: Any,
    defaultValue: () -> V,
) = if (key in this) {
    get(key) as V
} else {
    defaultValue().also { put(key, it) }
}

private fun <T> cachedReadOnlyProperty(block: BytecodePatchContext.(KProperty<*>) -> T) =
    object : ReadOnlyProperty<BytecodePatchContext, T> {
        private val cache = HashMap<BytecodePatchContext, T>(1)

        override fun getValue(
            thisRef: BytecodePatchContext,
            property: KProperty<*>,
        ) = if (thisRef in cache) {
            cache.getValue(thisRef)
        } else {
            cache.getOrPut(thisRef) { thisRef.block(property) }
        }
    }

class MutablePredicateList<T> internal constructor() : MutableList<Predicate<T>> by mutableListOf()

typealias DeclarativePredicate<T> = context(PredicateContext)
MutablePredicateList<T>.() -> Unit
typealias BytecodePatchContextDeclarativePredicate<T> = context(BytecodePatchContext, PredicateContext)
MutablePredicateList<T>.() -> Unit

fun <T> T.declarativePredicate(build: Function<MutablePredicateList<T>>) =
    with(MutablePredicateList<T>().apply(build)) {
        all(this@declarativePredicate)
    }

context(context: PredicateContext)
fun <T> T.rememberDeclarativePredicate(
    key: Any,
    block: Function<MutablePredicateList<T>>,
) = with(context.remember(key) { MutablePredicateList<T>().apply(block) }) {
    all(this@rememberDeclarativePredicate)
}

context(_: PredicateContext)
private fun <T> T.rememberDeclarativePredicate(predicate: DeclarativePredicate<T>) =
    rememberDeclarativePredicate("declarativePredicate") { predicate() }

@JvmName("firstMethodOrNullInMethods")
fun Iterable<Method>.firstMethodOrNull(methodReference: MethodReference) =
    firstOrNull { MethodUtil.methodSignaturesMatch(methodReference, it) }

@JvmName("firstMethodInMethods")
fun Iterable<Method>.firstMethod(methodReference: MethodReference) = requireNotNull(firstMethodOrNull(methodReference))

@JvmName("firstMutableMethodOrNullInMethods")
context(context: BytecodePatchContext)
fun Iterable<Method>.firstMutableMethodOrNull(methodReference: MethodReference) =
    firstMethodOrNull(methodReference)?.let { app.revanced.patcher.firstMutableMethod(it) }

@JvmName("firstMutableMethodInMethods")
context(_: BytecodePatchContext)
fun Iterable<Method>.firstMutableMethod(methodReference: MethodReference) = requireNotNull(firstMutableMethodOrNull(methodReference))

@JvmName("firstMethodOrNullInMethods")
fun Iterable<Method>.firstMethodOrNull(
    vararg strings: String,
    predicate: MethodPredicate = { true },
) = if (strings.isEmpty()) {
    withPredicateContext { firstOrNull { it.predicate() } }
} else {
    withPredicateContext {
        first { method ->
            val instructions = method.instructionsOrNull ?: return@first false

            // TODO: Check potential to optimize (Set or not).
            //  Maybe even use context maps, but the methods may not be present in the context yet.
            val methodStrings = instructions.asSequence().mapNotNull { it.string }.toSet()

            if (strings.any { it !in methodStrings }) return@first false

            method.predicate()
        }
    }
}

@JvmName("firstMethodInMethods")
fun Iterable<Method>.firstMethod(
    vararg strings: String,
    predicate: MethodPredicate = { true },
) = requireNotNull(firstMethodOrNull(strings = strings, predicate))

@JvmName("firstMutableMethodOrNullInMethods")
context(_: BytecodePatchContext)
fun Iterable<Method>.firstMutableMethodOrNull(
    vararg strings: String,
    predicate: MethodPredicate = { true },
) = firstMethodOrNull(
    strings = strings,
    predicate,
)?.let { app.revanced.patcher.firstMutableMethod(it) }

@JvmName("firstMutableMethodInMethods")
context(_: BytecodePatchContext)
fun Iterable<Method>.firstMutableMethod(
    vararg strings: String,
    predicate: MethodPredicate = { true },
) = requireNotNull(firstMutableMethodOrNull(strings = strings, predicate))

@JvmName("firstMethodDeclarativelyOrNullInMethods")
fun Iterable<Method>.firstMethodDeclarativelyOrNull(
    vararg strings: String,
    predicate: DeclarativePredicate<Method> = { },
) = firstMethodOrNull(strings = strings) { rememberDeclarativePredicate(predicate) }

@JvmName("firstMethodDeclarativelyInMethods")
fun Iterable<Method>.firstMethodDeclaratively(
    vararg strings: String,
    predicate: DeclarativePredicate<Method> = { },
) = requireNotNull(firstMethodDeclarativelyOrNull(strings = strings, predicate))

@JvmName("firstMutableMethodDeclarativelyOrNullInMethods")
context(_: BytecodePatchContext)
fun Iterable<Method>.firstMutableMethodDeclarativelyOrNull(
    vararg strings: String,
    predicate: DeclarativePredicate<Method> = { },
) = firstMutableMethodOrNull(strings = strings) { rememberDeclarativePredicate(predicate) }

@JvmName("firstMutableMethodDeclarativelyInMethods")
context(_: BytecodePatchContext)
fun Iterable<Method>.firstMutableMethodDeclaratively(
    vararg strings: String,
    predicate: DeclarativePredicate<Method> = { },
) = requireNotNull(firstMutableMethodDeclarativelyOrNull(strings = strings, predicate))

@JvmName("firstMethodOrNullInClassDefs")
fun Iterable<ClassDef>.firstMethodOrNull(methodReference: MethodReference) =
    asSequence().flatMap { it.methods.asSequence() }.asIterable().firstMethodOrNull(methodReference)

@JvmName("firstMethodInClassDefs")
fun Iterable<ClassDef>.firstMethod(methodReference: MethodReference) = requireNotNull(firstMethodOrNull(methodReference))

@JvmName("firstMutableMethodOrNullInClassDefs")
context(_: BytecodePatchContext)
fun Iterable<ClassDef>.firstMutableMethodOrNull(methodReference: MethodReference) =
    asSequence().flatMap { it.methods.asSequence() }.asIterable().firstMutableMethodOrNull(methodReference)

@JvmName("firstMutableMethodInClassDefs")
context(_: BytecodePatchContext)
fun Iterable<ClassDef>.firstMutableMethod(methodReference: MethodReference) = requireNotNull(firstMutableMethodOrNull(methodReference))

@JvmName("firstMethodOrNullInClassDefs")
fun Iterable<ClassDef>.firstMethodOrNull(predicate: MethodPredicate = { true }): Method? {
    forEach { classDef ->
        with(classDef) {
            classDef.methods.firstMethodOrNull { predicate() }?.let { return it }
        }
    }

    return null
}

@JvmName("firstMethodInClassDefs")
fun Iterable<ClassDef>.firstMethod(predicate: MethodPredicate = { true }) = requireNotNull(firstMethodOrNull(predicate))

@JvmName("firstMethodOrNullInClassDefs")
fun Iterable<ClassDef>.firstMethodOrNull(
    vararg strings: String,
    predicate: MethodPredicate = { true },
): Method? {
    forEach { classDef ->
        with(classDef) {
            classDef.methods.firstMethodOrNull(strings = strings) { predicate() }?.let { return it }
        }
    }

    return null
}

@JvmName("firstMethodInClassDefs")
fun Iterable<ClassDef>.firstMethod(
    vararg strings: String,
    predicate: MethodPredicate = { true },
) = requireNotNull(firstMethodOrNull(strings = strings, predicate))

@JvmName("firstMutableMethodOrNullInClassDefs")
context(_: BytecodePatchContext)
fun Iterable<ClassDef>.firstMutableMethodOrNull(
    vararg strings: String,
    predicate: MethodPredicate = { true },
) = firstMethodOrNull(
    strings = strings,
    predicate,
)?.let { app.revanced.patcher.firstMutableMethod(it) }

@JvmName("firstMutableMethodInClassDefs")
context(_: BytecodePatchContext)
fun Iterable<ClassDef>.firstMutableMethod(
    vararg strings: String,
    predicate: MethodPredicate = { true },
) = requireNotNull(firstMutableMethodOrNull(strings = strings, predicate))

@JvmName("firstMethodDeclarativelyOrNullInClassDefs")
fun Iterable<ClassDef>.firstMethodDeclarativelyOrNull(predicate: DeclarativePredicate<Method>) =
    firstMethodOrNull { rememberDeclarativePredicate(predicate) }

@JvmName("firstMethodDeclarativelyInClassDefs")
fun Iterable<ClassDef>.firstMethodDeclaratively(predicate: DeclarativePredicate<Method>) =
    requireNotNull(firstMethodDeclarativelyOrNull(predicate))

@JvmName("firstMethodDeclarativelyOrNullInClassDefs")
context(context: BytecodePatchContext)
fun Iterable<ClassDef>.firstMethodDeclarativelyOrNull(
    vararg strings: String,
    predicate: DeclarativePredicate<Method> = { },
) = firstMethodOrNull(strings = strings) { rememberDeclarativePredicate(predicate) }

@JvmName("firstMethodDeclarativelyInClassDefs")
context(context: BytecodePatchContext)
fun Iterable<ClassDef>.firstMethodDeclaratively(
    vararg strings: String,
    predicate: DeclarativePredicate<Method> = { },
) = requireNotNull(firstMethodDeclarativelyOrNull(strings = strings, predicate))

@JvmName("firstMutableMethodDeclarativelyOrNullInClassDefs")
context(context: BytecodePatchContext)
fun Iterable<ClassDef>.firstMutableMethodDeclarativelyOrNull(
    vararg strings: String,
    predicate: DeclarativePredicate<Method> = { },
) = firstMutableMethodOrNull(strings = strings) { rememberDeclarativePredicate(predicate) }

@JvmName("firstMethodOrNullInClassDef")
fun ClassDef.firstMethodOrNull(methodReference: MethodReference) = methods.firstMethodOrNull(methodReference)

@JvmName("firstMethodInClassDef")
fun ClassDef.firstMethod(methodReference: MethodReference) = requireNotNull(firstMethodOrNull(methodReference))

@JvmName("firstMutableMethodOrNullInClassDef")
context(_: BytecodePatchContext)
fun ClassDef.firstMutableMethodOrNull(methodReference: MethodReference) = methods.firstMutableMethodOrNull(methodReference)

@JvmName("firstMutableMethodInClassDef")
context(_: BytecodePatchContext)
fun ClassDef.firstMutableMethod(methodReference: MethodReference) = requireNotNull(firstMutableMethodOrNull(methodReference))

@JvmName("firstMethodOrNullInClassDef")
fun ClassDef.firstMethodOrNull(
    vararg strings: String,
    predicate: MethodPredicate = { true },
) = methods.firstMethodOrNull(strings = strings, predicate)

@JvmName("firstMethodInClassDef")
fun ClassDef.firstMethod(
    vararg strings: String,
    predicate: MethodPredicate = { true },
) = requireNotNull(firstMethodOrNull(strings = strings, predicate))

@JvmName("firstMutableMethodOrNullInClassDef")
context(_: BytecodePatchContext)
fun ClassDef.firstMutableMethodOrNull(
    vararg strings: String,
    predicate: MethodPredicate = { true },
) = methods.firstMutableMethodOrNull(strings = strings, predicate)

@JvmName("firstMutableMethodInClassDef")
context(_: BytecodePatchContext)
fun ClassDef.firstMutableMethod(
    vararg strings: String,
    predicate: MethodPredicate = { true },
) = requireNotNull(firstMutableMethodOrNull(strings = strings, predicate))

@JvmName("firstMethodDeclarativelyOrNullInClassDef")
fun ClassDef.firstMethodDeclarativelyOrNull(
    vararg strings: String,
    predicate: DeclarativePredicate<Method> = { },
) = methods.firstMethodDeclarativelyOrNull(strings = strings, predicate)

@JvmName("firstMethodDeclarativelyInClassDef")
fun ClassDef.firstMethodDeclaratively(
    vararg strings: String,
    predicate: DeclarativePredicate<Method> = { },
) = requireNotNull(firstMethodDeclarativelyOrNull(strings = strings, predicate))

@JvmName("firstMutableMethodDeclarativelyOrNullInClassDef")
context(_: BytecodePatchContext)
fun ClassDef.firstMutableMethodDeclarativelyOrNull(
    vararg strings: String,
    predicate: DeclarativePredicate<Method> = { },
) = methods.firstMutableMethodDeclarativelyOrNull(strings = strings, predicate)

@JvmName("firstMutableMethodDeclarativelyInClassDef")
context(_: BytecodePatchContext)
fun ClassDef.firstMutableMethodDeclaratively(
    vararg strings: String,
    predicate: DeclarativePredicate<Method> = { },
) = requireNotNull(firstMutableMethodDeclarativelyOrNull(strings = strings, predicate))

@JvmName("firstClassDefOrNullInClassDefs")
fun Iterable<ClassDef>.firstClassDefOrNull(
    type: String? = null,
    predicate: ClassDefPredicate = { true },
) = withPredicateContext {
    if (type == null) {
        firstOrNull { it.predicate() }
    } else {
        firstOrNull { it.type == type && it.predicate() }
    }
}

@JvmName("firstClassDefInClassDefs")
fun Iterable<ClassDef>.firstClassDef(
    type: String? = null,
    predicate: ClassDefPredicate = { true },
) = requireNotNull(firstClassDefOrNull(type, predicate))

@JvmName("firstMutableClassDefOrNullInClassDefs")
context(context: BytecodePatchContext)
fun Iterable<ClassDef>.firstMutableClassDefOrNull(
    type: String? = null,
    predicate: ClassDefPredicate = { true },
) = if (type == null) {
    firstClassDefOrNull(type, predicate)
} else {
    context.classDefs[type].takeIf { withPredicateContext { it?.predicate() == true } }
}?.let { context.classDefs.getOrReplaceMutable(it) }

@JvmName("firstMutableClassDefInClassDefs")
context(_: BytecodePatchContext)
fun Iterable<ClassDef>.firstMutableClassDef(
    type: String? = null,
    predicate: ClassDefPredicate = { true },
) = requireNotNull(firstMutableClassDefOrNull(type, predicate))

@JvmName("firstClassDefDeclarativelyOrNullInClassDefs")
fun Iterable<ClassDef>.firstClassDefDeclarativelyOrNull(
    type: String? = null,
    predicate: DeclarativePredicate<ClassDef> = { },
) = firstClassDefOrNull(type) { rememberDeclarativePredicate(predicate) }

@JvmName("firstClassDefDeclarativelyInClassDefs")
fun Iterable<ClassDef>.firstClassDefDeclaratively(
    type: String? = null,
    predicate: DeclarativePredicate<ClassDef> = { },
) = requireNotNull(firstClassDefDeclarativelyOrNull(type, predicate))

@JvmName("firstMutableClassDefDeclarativelyOrNullInClassDefs")
context(_: BytecodePatchContext)
fun Iterable<ClassDef>.firstMutableClassDefDeclarativelyOrNull(
    type: String? = null,
    predicate: DeclarativePredicate<ClassDef> = { },
) = firstMutableClassDefOrNull(type) { rememberDeclarativePredicate(predicate) }

@JvmName("firstMutableClassDefDeclarativelyInClassDefs")
context(_: BytecodePatchContext)
fun Iterable<ClassDef>.firstMutableClassDefDeclaratively(
    type: String? = null,
    predicate: DeclarativePredicate<ClassDef> = { },
) = requireNotNull(firstMutableClassDefDeclarativelyOrNull(type, predicate))

context(_: BytecodePatchContext)
fun firstMethodOrNull(methodReference: MethodReference) =
    firstClassDefOrNull(methodReference.definingClass)?.methods?.firstMethodOrNull(methodReference)

context(_: BytecodePatchContext)
fun firstMethod(method: MethodReference) = requireNotNull(firstMethodOrNull(method))

context(_: BytecodePatchContext)
fun firstMutableMethodOrNull(methodReference: MethodReference): MutableMethod? =
    firstMutableClassDefOrNull(methodReference.definingClass)?.methods?.first {
        MethodUtil.methodSignaturesMatch(
            methodReference,
            it,
        )
    }

context(_: BytecodePatchContext)
fun firstMutableMethod(method: MethodReference) = requireNotNull(firstMutableMethodOrNull(method))

context(context: BytecodePatchContext)
fun firstMethodOrNull(
    vararg strings: String,
    predicate: MethodPredicate = { true },
): Method? =
    withPredicateContext {
        if (strings.isEmpty()) return context.classDefs.firstMethodOrNull(predicate)

        // TODO: Get rid of duplicates, but this isn't needed for functionality. Perhaps worse performance-wise?
        val strings = strings.toSet()

        val methodsWithStrings = strings.mapNotNull { context.classDefs.methodsByString[it] }
        if (methodsWithStrings.size != strings.size) return null

        return methodsWithStrings.minBy { it.size }.firstOrNull { method ->
            val containsAllOtherStrings = methodsWithStrings.all { method in it }
            containsAllOtherStrings && method.predicate()
        }
    }

context(_: BytecodePatchContext)
fun firstMethod(
    vararg strings: String,
    predicate: MethodPredicate = { true },
) = requireNotNull(firstMethodOrNull(strings = strings, predicate))

context(_: BytecodePatchContext)
fun firstMutableMethodOrNull(
    vararg strings: String,
    predicate: MethodPredicate = { true },
) = firstMethodOrNull(strings = strings, predicate)?.let { method ->
    firstMutableMethodOrNull(method)
}

context(_: BytecodePatchContext)
fun firstMutableMethod(
    vararg strings: String,
    predicate: MethodPredicate = { true },
) = requireNotNull(firstMutableMethodOrNull(strings = strings, predicate))

fun gettingFirstMethodOrNull(method: MethodReference) = cachedReadOnlyProperty { firstMethodOrNull(method) }

fun gettingFirstMethod(method: MethodReference) = cachedReadOnlyProperty { firstMethod(method) }

fun gettingFirstMutableMethodOrNull(method: MethodReference) = cachedReadOnlyProperty { firstMutableMethodOrNull(method) }

fun gettingFirstMutableMethod(method: MethodReference) = cachedReadOnlyProperty { firstMutableMethod(method) }

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

context(_: BytecodePatchContext)
fun firstMethodDeclarativelyOrNull(
    vararg strings: String,
    predicate: DeclarativePredicate<Method> = { },
) = firstMethodOrNull(strings = strings) { rememberDeclarativePredicate(predicate) }

context(_: BytecodePatchContext)
fun firstMethodDeclaratively(
    vararg strings: String,
    predicate: DeclarativePredicate<Method> = { },
) = requireNotNull(firstMethodDeclarativelyOrNull(strings = strings, predicate))

context(_: BytecodePatchContext)
fun firstMutableMethodDeclarativelyOrNull(
    vararg strings: String,
    predicate: DeclarativePredicate<Method> = { },
) = firstMutableMethodOrNull(strings = strings) { rememberDeclarativePredicate(predicate) }

context(_: BytecodePatchContext)
fun firstMutableMethodDeclaratively(
    vararg strings: String,
    predicate: DeclarativePredicate<Method> = { },
) = requireNotNull(firstMutableMethodDeclarativelyOrNull(strings = strings, predicate))

fun gettingFirstMethodDeclarativelyOrNull(
    vararg strings: String,
    predicate: BytecodePatchContextDeclarativePredicate<Method> = { },
) = gettingFirstMethodOrNull(strings = strings) { rememberDeclarativePredicate { predicate() } }

fun gettingFirstMethodDeclaratively(
    vararg strings: String,
    predicate: BytecodePatchContextDeclarativePredicate<Method> = { },
) = gettingFirstMethod(strings = strings) { rememberDeclarativePredicate { predicate() } }

fun gettingFirstMutableMethodDeclarativelyOrNull(
    vararg strings: String,
    predicate: BytecodePatchContextDeclarativePredicate<Method> = { },
) = gettingFirstMutableMethodOrNull(strings = strings) { rememberDeclarativePredicate { predicate() } }

fun gettingFirstMutableMethodDeclaratively(
    vararg strings: String,
    predicate: BytecodePatchContextDeclarativePredicate<Method> = { },
) = gettingFirstMutableMethod(strings = strings) { rememberDeclarativePredicate { predicate() } }

context(context: BytecodePatchContext)
fun firstClassDefOrNull(
    type: String? = null,
    predicate: ClassDefPredicate = { true },
) = if (type == null) {
    context.classDefs.firstClassDefOrNull(type, predicate)
} else {
    withPredicateContext { context.classDefs[type]?.takeIf { it.predicate() } }
}

context(_: BytecodePatchContext)
fun firstClassDef(
    type: String? = null,
    predicate: ClassDefPredicate = { true },
) = requireNotNull(firstClassDefOrNull(type, predicate))

context(context: BytecodePatchContext)
fun firstMutableClassDefOrNull(
    type: String? = null,
    predicate: ClassDefPredicate = { true },
) = firstClassDefOrNull(type, predicate)?.let { context.classDefs.getOrReplaceMutable(it) }

context(_: BytecodePatchContext)
fun firstMutableClassDef(
    type: String? = null,
    predicate: ClassDefPredicate = { true },
) = requireNotNull(firstMutableClassDefOrNull(type, predicate))

fun gettingFirstClassDefOrNull(
    type: String? = null,
    predicate: BytecodePatchContextClassDefPredicate = { true },
) = cachedReadOnlyProperty { firstClassDefOrNull(type) { predicate() } }

fun gettingFirstClassDef(
    type: String? = null,
    predicate: BytecodePatchContextClassDefPredicate = { true },
) = cachedReadOnlyProperty { firstClassDef(type) { predicate() } }

fun gettingFirstMutableClassDefOrNull(
    type: String? = null,
    predicate: BytecodePatchContextClassDefPredicate = { true },
) = cachedReadOnlyProperty { firstMutableClassDefOrNull(type) { predicate() } }

fun gettingFirstMutableClassDef(
    type: String? = null,
    predicate: BytecodePatchContextClassDefPredicate = { true },
) = cachedReadOnlyProperty { firstMutableClassDef(type) { predicate() } }

context(_: BytecodePatchContext)
fun firstClassDefDeclarativelyOrNull(
    type: String? = null,
    predicate: DeclarativePredicate<ClassDef> = { },
) = firstClassDefOrNull(type) { rememberDeclarativePredicate(predicate) }

context(_: BytecodePatchContext)
fun firstClassDefDeclaratively(
    type: String? = null,
    predicate: DeclarativePredicate<ClassDef> = { },
) = firstClassDef(type) { rememberDeclarativePredicate(predicate) }

context(_: BytecodePatchContext)
fun firstMutableClassDefDeclarativelyOrNull(
    type: String? = null,
    predicate: DeclarativePredicate<ClassDef> = { },
) = firstMutableClassDefOrNull(type) { rememberDeclarativePredicate(predicate) }

context(_: BytecodePatchContext)
fun firstMutableClassDefDeclaratively(
    type: String? = null,
    predicate: DeclarativePredicate<ClassDef> = { },
) = firstMutableClassDef(type) { rememberDeclarativePredicate(predicate) }

fun gettingFirstClassDefDeclarativelyOrNull(
    type: String? = null,
    predicate: BytecodePatchContextDeclarativePredicate<ClassDef> = { },
) = gettingFirstClassDefOrNull { rememberDeclarativePredicate { predicate() } }

fun gettingFirstClassDefDeclaratively(
    type: String? = null,
    predicate: BytecodePatchContextDeclarativePredicate<ClassDef> = { },
) = gettingFirstClassDef { rememberDeclarativePredicate { predicate() } }

fun gettingFirstMutableClassDefDeclarativelyOrNull(
    type: String? = null,
    predicate: BytecodePatchContextDeclarativePredicate<ClassDef> = { },
) = gettingFirstMutableClassDefOrNull { rememberDeclarativePredicate { predicate() } }

fun gettingFirstMutableClassDefDeclaratively(
    type: String? = null,
    predicate: BytecodePatchContextDeclarativePredicate<ClassDef> = { },
) = gettingFirstMutableClassDef { rememberDeclarativePredicate { predicate() } }

class PredicateContext internal constructor() : MutableMap<Any, Any?> by mutableMapOf()

private inline fun <T> withPredicateContext(block: PredicateContext.() -> T) = PredicateContext().block()

typealias IndexedMatcherPredicate<T> = T.(lastMatchedIndex: Int, currentIndex: Int, setNextIndex: (Int?) -> Unit) -> Boolean

fun <T> indexedMatcher(vararg items: IndexedMatcherPredicate<T>) =
    IndexedMatcher<T>().apply {
        items.forEach { +it }
    }

fun <T> indexedMatcher(build: Function<IndexedMatcher<T>>) = IndexedMatcher<T>().apply(build)

fun <T> Iterable<T>.matchIndexed(build: Function<IndexedMatcher<T>>) = indexedMatcher(build)(this)

context(_: PredicateContext)
fun <T> Iterable<T>.matchIndexed(
    key: Any,
    build: Function<IndexedMatcher<T>>,
) = indexedMatcher<T>()(key, this, build)

context(_: PredicateContext)
fun <T> Iterable<T>.matchIndexed(
    key: Any,
    vararg items: IndexedMatcherPredicate<T>,
) = indexedMatcher<T>()(key, this) { items.forEach { +it } }

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

                // Try each unused predicate
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

context(context: PredicateContext)
inline operator fun <T, U, reified M : Matcher<T, U>> M.invoke(
    key: Any,
    iterable: Iterable<T>,
    builder: Function<M>,
) = context.remember(key) { apply(builder) }(iterable)

abstract class Matcher<T, U> : MutableList<U> by mutableListOf() {
    var matchIndex = -1
        protected set

    abstract operator fun invoke(haystack: Iterable<T>): Boolean
}

// endregion Matcher

// region MutablePredicateList extensions

fun <T> MutablePredicateList<T>.allOf(block: Function<MutablePredicateList<T>>) {
    val child = MutablePredicateList<T>().apply(block)
    add { child.all { it() } }
}

fun <T> MutablePredicateList<T>.anyOf(block: Function<MutablePredicateList<T>>) {
    val child = MutablePredicateList<T>().apply(block)
    add { child.any { it() } }
}

fun <T> MutablePredicateList<T>.noneOf(block: Function<MutablePredicateList<T>>) {
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
    compare: String.(String) -> Boolean = String::startsWith,
) = predicate { this.returnType.compare(returnType) }

fun MutablePredicateList<Method>.name(predicate: Predicate<String>) = predicate { name.predicate() }

fun MutablePredicateList<Method>.name(
    name: String,
    compare: String.(String) -> Boolean = String::equals,
) = predicate { this.name.compare(name) }

fun MutablePredicateList<Method>.definingClass(predicate: Predicate<String>) = predicate { definingClass.predicate() }

fun MutablePredicateList<Method>.definingClass(
    definingClass: String,
    compare: String.(String) -> Boolean = String::equals,
) = predicate { this.definingClass.compare(definingClass) }

fun MutablePredicateList<Method>.parameterTypes(vararg parameterTypePrefixes: String) =
    predicate {
        parameterTypes.size == parameterTypePrefixes.size &&
            parameterTypes
                .zip(parameterTypePrefixes)
                .all { (a, b) -> a.startsWith(b) }
    }

fun MutablePredicateList<Method>.strings(build: Function<IndexedMatcher<Instruction>>) {
    val match = indexedMatcher(build)

    predicate { implementation { match(instructions) } }
}

context(matcher: IndexedMatcher<Instruction>)
fun MutablePredicateList<Method>.strings(build: Function<IndexedMatcher<Instruction>>) {
    matcher.build()

    predicate { implementation { matcher(instructions) } }
}

// fun MutablePredicateList<Method>.strings(
//    vararg predicates: IndexedMatcherPredicate<Instruction>
// ) = strings { predicates.forEach { +it } }
//
// context(matcher: IndexedMatcher<Instruction>)
// fun MutablePredicateList<Method>.strings(
//    vararg predicates: IndexedMatcherPredicate<Instruction>
// ) = strings { predicates.forEach { +it } }
//
//
// fun MutablePredicateList<Method>.strings(
//    vararg strings: String
// ) = strings(predicates = strings.map { string(it) }.toTypedArray())
//
// context(
//    stringsList: MutableList<String>,
//    matcher: IndexedMatcherPredicate<Instruction>)
// fun MutablePredicateList<Method>.strings(
//    vararg strings: String
// ) {
//    stringsList += strings
//
//    strings(predicates = strings.map { string(it) }.toTypedArray())
// }

fun MutablePredicateList<Method>.instructions(build: Function<IndexedMatcher<Instruction>>) {
    val match = indexedMatcher(build)

    predicate { implementation { match(instructions) } }
}

fun MutablePredicateList<Method>.instructions(vararg predicates: IndexedMatcherPredicate<Instruction>) =
    instructions {
        predicates.forEach { +it }
    }

context(matcher: IndexedMatcher<Instruction>)
fun MutablePredicateList<Method>.instructions(build: Function<IndexedMatcher<Instruction>>) {
    matcher.build()

    predicate { implementation { matcher(instructions) } }
}

context(matcher: IndexedMatcher<Instruction>)
fun MutablePredicateList<Method>.instructions(vararg predicates: IndexedMatcherPredicate<Instruction>) =
    instructions { predicates.forEach { +it } }

fun MutablePredicateList<Method>.instructions(vararg predicates: Predicate<Instruction>) =
    instructions { predicates.forEach { add { _, _, _ -> it() } } }

fun MutablePredicateList<Method>.custom(block: Predicate<Method>) {
    predicate { block() }
}

fun MutablePredicateList<Method>.opcodes(vararg opcodes: Opcode) = instructions { opcodes.forEach { +it() } }

context(matcher: IndexedMatcher<Instruction>)
fun MutablePredicateList<Method>.opcodes(vararg opcodes: Opcode) = instructions { opcodes.forEach { +it() } }

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
    compare: String.(String) -> Boolean = String::equals,
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
    compare: String.(type: String) -> Boolean = String::equals,
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

typealias BuildCompositeDeclarativePredicate<Method> =
    context(
        BytecodePatchContext,
        PredicateContext,
        IndexedMatcher<Instruction>,
        MutableList<String>
    )
    MutablePredicateList<Method>.() -> Unit

fun firstMethodComposite(
    vararg strings: String,
    build: BuildCompositeDeclarativePredicate<Method> = { },
) = MatchBuilder(strings = strings, build)

class MatchBuilder private constructor(
    private val strings: MutableList<String>,
    indexedMatcher: IndexedMatcher<Instruction> = indexedMatcher(),
    build: BuildCompositeDeclarativePredicate<Method> = { },
) {
    internal constructor(
        vararg strings: String,
        build: BuildCompositeDeclarativePredicate<Method> = { },
    ) : this(strings = mutableListOf(elements = strings), build = build)

    private val predicate: BytecodePatchContextDeclarativePredicate<Method> = {
        context(strings, indexedMatcher) { build() }
    }

    val indices = indexedMatcher.indices

    private val BytecodePatchContext.cachedImmutableMethodOrNull by gettingFirstMethodDeclarativelyOrNull(
        strings = strings.toTypedArray(),
        predicate,
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
    fun match(classDef: ClassDef) =
        Match(
            context,
            classDef.firstMethodDeclarativelyOrNull { predicate() },
            indices,
        )
}

class Match(
    val context: BytecodePatchContext,
    val immutableMethodOrNull: Method?,
    val indices: List<Int>,
) {
    val immutableMethod by lazy { requireNotNull(immutableMethodOrNull) }

    val methodOrNull by lazy {
        context(context) { firstMutableMethodOrNull(immutableMethodOrNull ?: return@lazy null) }
    }

    val method by lazy { requireNotNull(methodOrNull) }

    val immutableClassDefOrNull by lazy { context(context) { immutableMethodOrNull?.immutableClassDefOrNull } }

    val immutableClassDef by lazy { requireNotNull(context(context) { immutableMethod.immutableClassDef }) }

    val classDefOrNull by lazy {
        context(context) { firstMutableClassDefOrNull(immutableMethodOrNull?.definingClass ?: return@lazy null) }
    }

    val classDef by lazy { requireNotNull(classDefOrNull) }
}

context(context: BytecodePatchContext)
val Method.immutableClassDefOrNull get() = firstClassDefOrNull(definingClass)

context(_: BytecodePatchContext)
val Method.immutableClassDef get() = requireNotNull(immutableClassDefOrNull)

context(_: BytecodePatchContext)
val Method.classDefOrNull get() = firstMutableClassDefOrNull(definingClass)

context(_: BytecodePatchContext)
val Method.classDef get() = requireNotNull(classDefOrNull)
