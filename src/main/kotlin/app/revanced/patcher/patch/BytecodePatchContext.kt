package app.revanced.patcher.patch

import app.revanced.patcher.InternalApi
import app.revanced.patcher.PatcherConfig
import app.revanced.patcher.PatcherResult
import app.revanced.patcher.dex.mutable.MutableClassDef
import app.revanced.patcher.dex.mutable.MutableClassDef.Companion.toMutable
import app.revanced.patcher.extensions.instructionsOrNull
import app.revanced.patcher.util.ClassMerger.merge
import app.revanced.patcher.util.MethodNavigator
import com.android.tools.smali.dexlib2.Opcode
import com.android.tools.smali.dexlib2.Opcodes
import com.android.tools.smali.dexlib2.iface.ClassDef
import com.android.tools.smali.dexlib2.iface.DexFile
import com.android.tools.smali.dexlib2.iface.Method
import com.android.tools.smali.dexlib2.iface.instruction.ReferenceInstruction
import com.android.tools.smali.dexlib2.iface.reference.MethodReference
import com.android.tools.smali.dexlib2.iface.reference.StringReference
import lanchon.multidexlib2.BasicDexFileNamer
import lanchon.multidexlib2.DexIO
import lanchon.multidexlib2.MultiDexIO
import lanchon.multidexlib2.RawDexIO
import java.io.Closeable
import java.util.*
import java.util.logging.Logger

/**
 * A context for patches containing the current state of the bytecode.
 *
 * @param config The [PatcherConfig] used to create this context.
 */
@Suppress("MemberVisibilityCanBePrivate")
class BytecodePatchContext internal constructor(private val config: PatcherConfig) :
    PatchContext<Set<PatcherResult.PatchedDexFile>>,
    Closeable {
    private val logger = Logger.getLogger(this::class.java.name)

    /**
     * [Opcodes] of the supplied [PatcherConfig.apkFile].
     */
    internal val opcodes: Opcodes

    /**
     * The list of classes.
     */
    val classDefs = MultiDexIO.readDexFile(
        true,
        config.apkFile,
        BasicDexFileNamer(),
        null,
        null,
    ).also { opcodes = it.opcodes }.classes.toMutableSet()

    /**
     * The lookup maps for methods and the class they are a member of from the [classDefs].
     */
    internal val lookupMaps by lazy { LookupMaps(classDefs) }

    /**
     * Merge the extension of [bytecodePatch] into the [BytecodePatchContext].
     * If no extension is present, the function will return early.
     *
     * @param bytecodePatch The [BytecodePatch] to merge the extension of.
     */
    internal fun mergeExtension(bytecodePatch: BytecodePatch) {
        bytecodePatch.extensionInputStream?.get()?.use { extensionStream ->
            RawDexIO.readRawDexFile(extensionStream, 0, null).classes.forEach { classDef ->
                val existingClass = lookupMaps.classesByType[classDef.type] ?: run {
                    logger.fine { "Adding class \"$classDef\"" }

                    classDefs += classDef
                    lookupMaps.classesByType[classDef.type] = classDef

                    return@forEach
                }

                logger.fine { "Class \"$classDef\" exists already. Adding missing methods and fields." }

                existingClass.merge(classDef, this@BytecodePatchContext).let { mergedClass ->
                    // If the class was merged, replace the original class with the merged class.
                    if (mergedClass === existingClass) {
                        return@let
                    }

                    classDefs -= existingClass
                    classDefs += mergedClass
                }
            }
        } ?: logger.fine("No extension to merge")
    }

    /**
     * Convert a [ClassDef] to a [MutableClassDef].
     * If the [ClassDef] is already a [MutableClassDef], it is returned as is.
     *
     * @return The mutable version of the [ClassDef].
     */
    fun ClassDef.mutable(): MutableClassDef =
        this as? MutableClassDef ?: also(classDefs::remove).toMutable().also(classDefs::add)

    /**
     * Navigate a method.
     *
     * @param method The method to navigate.
     *
     * @return A [MethodNavigator] for the method.
     */
    fun navigate(method: MethodReference) = MethodNavigator(method)

    /**
     * Compile bytecode from the [BytecodePatchContext].
     *
     * @return The compiled bytecode.
     */
    @InternalApi
    override fun get(): Set<PatcherResult.PatchedDexFile> {
        logger.info("Compiling patched dex files")

        // Free up memory before compiling the dex files.
        lookupMaps.close()

        val patchedDexFileResults =
            config.patchedFiles.resolve("dex").also {
                it.deleteRecursively() // Make sure the directory is empty.
                it.mkdirs()
            }.apply {
                MultiDexIO.writeDexFile(
                    true,
                    -1,
                    this,
                    BasicDexFileNamer(),
                    object : DexFile {
                        override fun getClasses() =
                            this@BytecodePatchContext.classDefs.toSet()

                        override fun getOpcodes() = this@BytecodePatchContext.opcodes
                    },
                    DexIO.DEFAULT_MAX_DEX_POOL_SIZE,
                ) { _, entryName, _ -> logger.info { "Compiled $entryName" } }
            }.listFiles { it.isFile }!!.map {
                PatcherResult.PatchedDexFile(it.name, it.inputStream())
            }.toSet()

        System.gc()

        return patchedDexFileResults
    }

    /**
     * A lookup map for methods and the class they are a member of and classes.
     *
     * @param classDefs The list of classes to create the lookup maps from.
     */
    internal class LookupMaps internal constructor(classDefs: Set<ClassDef>) : Closeable {
        /**
         * Methods associated by strings referenced in it.
         */
        internal val methodsByStrings = MethodClassPairsLookupMap()

        // Lookup map for fast checking if a class exists by its type.
        val classesByType = mutableMapOf<String, ClassDef>().apply {
            classDefs.forEach { classDef -> put(classDef.type, classDef) }
        }

        init {
            classDefs.forEach { classDef ->
                classDef.methods.forEach { method ->
                    val methodClassPair: MethodClassPair = method to classDef

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

        override fun close() {
            methodsByStrings.clear()
            classesByType.clear()
        }
    }

    override fun close() {
        lookupMaps.close()
        classDefs.clear()
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
