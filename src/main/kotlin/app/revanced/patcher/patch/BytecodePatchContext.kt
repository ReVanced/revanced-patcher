package app.revanced.patcher.patch

import app.revanced.patcher.InternalApi
import app.revanced.patcher.PatcherConfig
import app.revanced.patcher.PatcherResult
import app.revanced.patcher.extensions.InstructionExtensions.instructionsOrNull
import app.revanced.patcher.util.ClassMerger.merge
import app.revanced.patcher.util.MethodNavigator
import app.revanced.patcher.util.ProxyClassList
import app.revanced.patcher.util.proxy.ClassProxy
import com.android.tools.smali.dexlib2.Opcode
import com.android.tools.smali.dexlib2.Opcodes
import com.android.tools.smali.dexlib2.iface.ClassDef
import com.android.tools.smali.dexlib2.iface.DexFile
import com.android.tools.smali.dexlib2.iface.Method
import com.android.tools.smali.dexlib2.iface.instruction.ReferenceInstruction
import com.android.tools.smali.dexlib2.iface.reference.StringReference
import lanchon.multidexlib2.BasicDexFileNamer
import lanchon.multidexlib2.DexIO
import lanchon.multidexlib2.MultiDexIO
import lanchon.multidexlib2.RawDexIO
import java.io.Closeable
import java.io.FileFilter
import java.util.*
import java.util.logging.Logger

/**
 * A context for patches containing the current state of the bytecode.
 *
 * @param config The [PatcherConfig] used to create this context.
 */
@Suppress("MemberVisibilityCanBePrivate")
class BytecodePatchContext internal constructor(private val config: PatcherConfig) : PatchContext<Set<PatcherResult.PatchedDexFile>> {
    private val logger = Logger.getLogger(BytecodePatchContext::class.java.name)

    /**
     * [Opcodes] of the supplied [PatcherConfig.apkFile].
     */
    internal val opcodes: Opcodes

    /**
     * The list of classes.
     */
    val classes = ProxyClassList(
        MultiDexIO.readDexFile(
            true,
            config.apkFile,
            BasicDexFileNamer(),
            null,
            null,
        ).also { opcodes = it.opcodes }.classes.toMutableList(),
    )

    /**
     * The lookup maps for methods and the class they are a member of from the [classes].
     */
    internal val lookupMaps by lazy { LookupMaps(classes) }

    /**
     * Merge the extension to [classes].
     */
    internal fun mergeExtension(extension: ByteArray) {
        RawDexIO.readRawDexFile(extension, 0, null).classes.forEach { classDef ->
            val existingClass = lookupMaps.classesByType[classDef.type] ?: run {
                logger.fine("Adding $classDef")

                lookupMaps.classesByType[classDef.type] = classDef
                classes += classDef

                return@forEach
            }

            logger.fine("$classDef exists. Adding missing methods and fields.")

            existingClass.merge(classDef, this@BytecodePatchContext).let { mergedClass ->
                // If the class was merged, replace the original class with the merged class.
                if (mergedClass === existingClass) {
                    return@let
                }

                classes -= existingClass
                classes += mergedClass
            }
        }
    }

    /**
     * Find a class by its type using a contains check.
     *
     * @param type The type of the class.
     * @return A proxy for the first class that matches the type.
     */
    fun classByType(type: String) = classBy { type in it.type }

    /**
     * Find a class with a predicate.
     *
     * @param predicate A predicate to match the class.
     * @return A proxy for the first class that matches the predicate.
     */
    fun classBy(predicate: (ClassDef) -> Boolean) =
        classes.proxyPool.find { predicate(it.immutableClass) } ?: classes.find(predicate)?.let { proxy(it) }

    /**
     * Proxy the class to allow mutation.
     *
     * @param classDef The class to proxy.
     *
     * @return A proxy for the class.
     */
    fun proxy(classDef: ClassDef) = this@BytecodePatchContext.classes.proxyPool.find {
        it.immutableClass.type == classDef.type
    } ?: ClassProxy(classDef).also { this@BytecodePatchContext.classes.proxyPool.add(it) }

    /**
     * Navigate a method.
     *
     * @param method The method to navigate.
     *
     * @return A [MethodNavigator] for the method.
     */
    fun navigate(method: Method) = MethodNavigator(this@BytecodePatchContext, method)

    /**
     * Compile bytecode from the [BytecodePatchContext].
     *
     * @return The compiled bytecode.
     */
    @InternalApi
    override fun get(): Set<PatcherResult.PatchedDexFile> {
        logger.info("Compiling patched dex files")

        val patchedDexFileResults =
            config.patchedFiles.resolve("dex").also {
                it.deleteRecursively() // Make sure the directory is empty.
                it.mkdirs()
            }.apply {
                MultiDexIO.writeDexFile(
                    true,
                    if (config.multithreadingDexFileWriter) -1 else 1,
                    this,
                    BasicDexFileNamer(),
                    object : DexFile {
                        override fun getClasses() =
                            this@BytecodePatchContext.classes.also(ProxyClassList::replaceClasses).toSet()

                        override fun getOpcodes() = this@BytecodePatchContext.opcodes
                    },
                    DexIO.DEFAULT_MAX_DEX_POOL_SIZE,
                ) { _, entryName, _ -> logger.info("Compiled $entryName") }
            }.listFiles(FileFilter { it.isFile })!!.map {
                PatcherResult.PatchedDexFile(it.name, it.inputStream())
            }.toSet()

        System.gc()

        return patchedDexFileResults
    }

    /**
     * A lookup map for methods and the class they are a member of and classes.
     *
     * @param classes The list of classes to create the lookup maps from.
     */
    internal class LookupMaps internal constructor(classes: List<ClassDef>) : Closeable {
        /**
         * Classes associated by their type.
         */
        internal val classesByType = classes.associateBy { it.type }.toMutableMap()

        /**
         * All methods and the class they are a member of.
         */
        internal val allMethods = MethodClassPairs()

        /**
         * Methods associated by its access flags, return type and parameter.
         */
        internal val methodsBySignature = MethodClassPairsLookupMap()

        /**
         * Methods associated by strings referenced in it.
         */
        internal val methodsByStrings = MethodClassPairsLookupMap()

        init {
            classes.forEach { classDef ->
                classDef.methods.forEach { method ->
                    val methodClassPair: MethodClassPair = method to classDef

                    // For fingerprints with no access or return type specified.
                    allMethods += methodClassPair

                    val accessFlagsReturnKey = method.accessFlags.toString() + method.returnType.first()

                    // Add <access><returnType> as the key.
                    methodsBySignature[accessFlagsReturnKey] = methodClassPair

                    // Add <access><returnType>[parameters] as the key.
                    methodsBySignature[
                        buildString {
                            append(accessFlagsReturnKey)
                            appendParameters(method.parameterTypes)
                        },
                    ] = methodClassPair

                    // Add strings contained in the method as the key.
                    method.instructionsOrNull?.forEach instructions@{ instruction ->
                        if (instruction.opcode != Opcode.CONST_STRING && instruction.opcode != Opcode.CONST_STRING_JUMBO) {
                            return@instructions
                        }

                        val string = ((instruction as ReferenceInstruction).reference as StringReference).string

                        methodsByStrings[string] = methodClassPair
                    }

                    // In the future, the class type could be added to the lookup map.
                    // This would require MethodFingerprint to be changed to include the class type.
                }
            }
        }

        internal companion object {
            /**
             * Appends a string based on the parameter reference types of this method.
             */
            internal fun StringBuilder.appendParameters(parameters: Iterable<CharSequence>) {
                // Maximum parameters to use in the signature key.
                // Some apps have methods with an incredible number of parameters (over 100 parameters have been seen).
                // To keep the signature map from becoming needlessly bloated,
                // group together in the same map entry all methods with the same access/return and 5 or more parameters.
                // The value of 5 was chosen based on local performance testing and is not set in stone.
                val maxSignatureParameters = 5
                // Must append a unique value before the parameters to distinguish this key includes the parameters.
                // If this is not appended, then methods with no parameters
                // will collide with different keys that specify access/return but omit the parameters.
                append("p:")
                parameters.forEachIndexed { index, parameter ->
                    if (index >= maxSignatureParameters) return
                    append(parameter.first())
                }
            }
        }

        override fun close() {
            allMethods.clear()
            methodsBySignature.clear()
            methodsByStrings.clear()
        }
    }
}

/**
 * A pair of a [Method] and the [ClassDef] it is a member of.
 */
internal typealias MethodClassPair = Pair<Method, ClassDef>

/**
 * A list of [MethodClassPair]s.
 */
internal typealias MethodClassPairs = LinkedList<MethodClassPair>

/**
 * A lookup map for [MethodClassPairs]s.
 * The key is a string and the value is a list of [MethodClassPair]s.
 */
internal class MethodClassPairsLookupMap : MutableMap<String, MethodClassPairs> by mutableMapOf() {
    /**
     * Add a [MethodClassPair] associated by any key.
     * If the key does not exist, a new list is created and the [MethodClassPair] is added to it.
     */
    internal operator fun set(key: String, methodClassPair: MethodClassPair) =
        apply { getOrPut(key) { MethodClassPairs() }.add(methodClassPair) }
}
