@file:Suppress("unused", "MemberVisibilityCanBePrivate")

package app.revanced.patcher

import app.revanced.patcher.dex.mutable.MutableClassDef.Companion.toMutable
import app.revanced.patcher.dex.mutable.MutableMethod
import app.revanced.patcher.dex.mutable.MutableMethod.Companion.toMutable
import app.revanced.patcher.extensions.addInstructions
import app.revanced.patcher.patch.BytecodePatchContext
import app.revanced.patcher.patch.bytecodePatch
import app.revanced.patcher.patch.gettingBytecodePatch
import com.android.tools.smali.dexlib2.HiddenApiRestriction
import com.android.tools.smali.dexlib2.Opcode
import com.android.tools.smali.dexlib2.iface.Annotation
import com.android.tools.smali.dexlib2.iface.ClassDef
import com.android.tools.smali.dexlib2.iface.ExceptionHandler
import com.android.tools.smali.dexlib2.iface.Field
import com.android.tools.smali.dexlib2.iface.Method
import com.android.tools.smali.dexlib2.iface.MethodImplementation
import com.android.tools.smali.dexlib2.iface.MethodParameter
import com.android.tools.smali.dexlib2.iface.TryBlock
import com.android.tools.smali.dexlib2.iface.instruction.Instruction
import com.android.tools.smali.dexlib2.util.InstructionUtil
import com.android.tools.smali.dexlib2.util.MethodUtil
import kotlin.collections.any
import kotlin.properties.ReadOnlyProperty

fun Iterable<ClassDef>.anyClassDef(predicate: ClassDef.() -> Boolean) =
    any(predicate)

fun ClassDef.anyMethod(predicate: Method.() -> Boolean) =
    methods.any(predicate)

fun ClassDef.anyDirectMethod(predicate: Method.() -> Boolean) =
    directMethods.any(predicate)

fun ClassDef.anyVirtualMethod(predicate: Method.() -> Boolean) =
    virtualMethods.any(predicate)

fun ClassDef.anyField(predicate: Field.() -> Boolean) =
    fields.any(predicate)

fun ClassDef.anyInstanceField(predicate: Field.() -> Boolean) =
    instanceFields.any(predicate)

fun ClassDef.anyStaticField(predicate: Field.() -> Boolean) =
    staticFields.any(predicate)

fun ClassDef.anyInterface(predicate: String.() -> Boolean) =
    interfaces.any(predicate)

fun ClassDef.anyAnnotation(predicate: Annotation.() -> Boolean) =
    annotations.any(predicate)

fun Method.implementation(predicate: MethodImplementation.() -> Boolean) =
    implementation?.predicate() ?: false

fun Method.anyParameter(predicate: MethodParameter.() -> Boolean) =
    parameters.any(predicate)

fun Method.anyParameterType(predicate: CharSequence.() -> Boolean) =
    parameterTypes.any(predicate)

fun Method.anyAnnotation(predicate: Annotation.() -> Boolean) =
    annotations.any(predicate)

fun Method.anyHiddenApiRestriction(predicate: HiddenApiRestriction.() -> Boolean) =
    hiddenApiRestrictions.any(predicate)

fun MethodImplementation.anyInstruction(predicate: Instruction.() -> Boolean) =
    instructions.any(predicate)

fun MethodImplementation.anyTryBlock(predicate: TryBlock<out ExceptionHandler>.() -> Boolean) =
    tryBlocks.any(predicate)

fun MethodImplementation.anyDebugItem(predicate: Any.() -> Boolean) =
    debugItems.any(predicate)

fun Iterable<Instruction>.anyInstruction(predicate: Instruction.() -> Boolean) =
    any(predicate)


fun BytecodePatchContext.firstClassDefOrNull(predicate: ClassDef.() -> Boolean) =
    classDefs.firstOrNull { predicate(it) }

fun BytecodePatchContext.firstClassDef(predicate: ClassDef.() -> Boolean) =
    requireNotNull(firstClassDefOrNull(predicate))

fun BytecodePatchContext.firstMutableClassDefOrNull(predicate: ClassDef.() -> Boolean) =
    firstClassDefOrNull(predicate)?.mutable()

fun BytecodePatchContext.firstMutableClassDef(predicate: ClassDef.() -> Boolean) =
    requireNotNull(firstMutableClassDefOrNull(predicate))

fun BytecodePatchContext.firstMethodOrNull(predicate: Method.() -> Boolean) =
    classDefs.asSequence().flatMap { it.methods.asSequence() }
        .firstOrNull { predicate(it) }

fun BytecodePatchContext.firstMethod(predicate: Method.() -> Boolean) =
    requireNotNull(firstMethodOrNull(predicate))

fun BytecodePatchContext.firstMutableMethodOrNull(predicate: Method.() -> Boolean): MutableMethod? {
    classDefs.forEach { classDef ->
        classDef.methods.forEach { method ->
            if (predicate(method)) return classDef.mutable().methods.first {
                MethodUtil.methodSignaturesMatch(it, method)
            }
        }
    }

    return null
}

fun BytecodePatchContext.firstMutableMethod(predicate: Method.() -> Boolean) =
    requireNotNull(firstMutableMethodOrNull(predicate))

fun gettingFirstClassDefOrNull(predicate: ClassDef.() -> Boolean) = ReadOnlyProperty<Any?, ClassDef?> { thisRef, _ ->
    require(thisRef is BytecodePatchContext)

    thisRef.firstClassDefOrNull(predicate)
}

fun gettingFirstClassDef(predicate: ClassDef.() -> Boolean) = requireNotNull(gettingFirstClassDefOrNull(predicate))

fun gettingFirstMutableClassDefOrNull(predicate: ClassDef.() -> Boolean) =
    ReadOnlyProperty<Any?, ClassDef?> { thisRef, _ ->
        require(thisRef is BytecodePatchContext)

        thisRef.firstMutableClassDefOrNull(predicate)
    }

fun gettingFirstMutableClassDef(predicate: ClassDef.() -> Boolean) =
    requireNotNull(gettingFirstMutableClassDefOrNull(predicate))

fun gettingFirstMethodOrNull(predicate: Method.() -> Boolean) = ReadOnlyProperty<Any?, Method?> { thisRef, _ ->
    require(thisRef is BytecodePatchContext)

    thisRef.firstMethodOrNull(predicate)
}

fun gettingFirstMethod(predicate: Method.() -> Boolean) = requireNotNull(gettingFirstMethodOrNull(predicate))

fun gettingFirstMutableMethodOrNull(predicate: Method.() -> Boolean) = ReadOnlyProperty<Any?, Method?> { thisRef, _ ->
    require(thisRef is BytecodePatchContext)

    thisRef.firstMutableMethodOrNull(predicate)
}

fun gettingFirstMutableMethod(predicate: Method.() -> Boolean) =
    requireNotNull(gettingFirstMutableMethodOrNull(predicate))

val classDefOrNull by gettingFirstClassDefOrNull { true }
val classDef by gettingFirstClassDef { true }
val mutableClassDefOrNull by gettingFirstMutableClassDefOrNull { true }
val mutableClassDef by gettingFirstMutableClassDef { true }
val methodOrNull by gettingFirstMethodOrNull { true }
val methodDef by gettingFirstMethod { true }
val mutableMethodOrNull by gettingFirstMutableMethodOrNull { true }
val mutableMethodDef by gettingFirstMutableMethod { true }

val `My Patch` by gettingBytecodePatch {
    execute {
        val classDefOrNull = firstClassDefOrNull { true }
        val classDef = firstClassDef { true }
        val mutableClassDefOrNull = firstMutableClassDefOrNull { true }
        val mutableClassDef = firstMutableClassDef { true }
        val methodOrNull = firstMethodOrNull { true }
        val method = firstMethod { true }
        val mutableMethodOrNull = firstMutableMethodOrNull { true }
        val mutableMethod = firstMutableMethod { true }
        val apiTest = firstMethod {
            implementation {
                instructions.matchSequentially<Instruction> {
                    add { opcode == Opcode.RETURN_VOID }
                    add { opcode == Opcode.NOP }
                }
            }
        }

        fun Method.matchSequentiallyInstructions(
            builder: MutableList<Instruction.() -> Boolean>.() -> Unit
        ) = implementation?.instructions?.matchSequentially(builder) ?: false

        firstMutableMethod {
            matchSequentiallyInstructions {
                add { opcode == Opcode.RETURN_VOID }
                add { opcode == Opcode.NOP }
            }
        }.addInstructions("apiTest2")
    }
}


abstract class Matcher<T> : MutableList<T.() -> Boolean> by mutableListOf() {
    var matchIndex = -1
        protected set

    abstract operator fun invoke(haystack: Iterable<T>): Boolean
}


open class SequentialMatcher<T> internal constructor() : Matcher<T>() {
    override operator fun invoke(haystack: Iterable<T>) = true
}

class CaptureStringIndices<T> internal constructor() : Matcher<T>() {
    val capturedStrings = mutableMapOf<String>()

    override operator fun invoke(haystack: Iterable<T>) {

    }
}

fun <T> matchSequentially() = SequentialMatcher<T>()
fun <T> sequentialMatcher(builder: MutableList<T.() -> Boolean>.() -> Unit) =
    SequentialMatcher<T>().apply(builder)

fun <T> Iterable<T>.matchSequentially(builder: MutableList<T.() -> Boolean>.() -> Unit) =
    sequentialMatcher(builder)(this)
