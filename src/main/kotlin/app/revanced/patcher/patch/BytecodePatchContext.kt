package app.revanced.patcher.patch

import app.revanced.patcher.InternalApi
import app.revanced.patcher.PatcherConfig
import app.revanced.patcher.PatcherResult
import app.revanced.patcher.extensions.InstructionExtensions.instructionsOrNull
import app.revanced.patcher.util.ClassMerger.merge
import app.revanced.patcher.util.MethodNavigator
import app.revanced.patcher.util.PatchClasses
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
import java.io.FileFilter
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
     * All classes for the target app and any extension classes.
     */
    val classes = PatchClasses(
        MultiDexIO.readDexFile(
            true,
            config.apkFile,
            BasicDexFileNamer(),
            null,
            null,
        ).also { opcodes = it.opcodes }.classes
    )

    /**
     * The lookup maps for methods and the class they are a member of from the [classes].
     */
    internal val lookupMaps by lazy { LookupMaps(classes.pool.values) }

    /**
     * Merge the extension of [bytecodePatch] into the [BytecodePatchContext].
     * If no extension is present, the function will return early.
     *
     * @param bytecodePatch The [BytecodePatch] to merge the extension of.
     */
    internal fun mergeExtension(bytecodePatch: BytecodePatch) {
        bytecodePatch.extensionInputStream?.get()?.use { extensionStream ->
            RawDexIO.readRawDexFile(extensionStream, 0, null).classes.forEach { classDef ->
                val existingClass = classes.classByOrNull(classDef.type) ?: run {
                    logger.fine { "Adding class \"$classDef\"" }

                    classes.addClass(classDef)

                    return@forEach
                }

                logger.fine { "Class \"$classDef\" exists already. Adding missing methods and fields." }

                existingClass.merge(classDef, this@BytecodePatchContext).let { mergedClass ->
                    // If the class was merged, replace the original class with the merged class.
                    if (mergedClass === existingClass) {
                        return@let
                    }

                    classes.addClass(mergedClass)
                }
            }
        } ?: logger.fine("No extension to merge")
    }

    /**
     * Find a class with a predicate.
     *
     * @param classType The full classname.
     * @return An immutable instance of the class type.
     * @see mutableClassBy
     */
    fun classBy(classType: String) = classes.classBy(classType)

    /**
     * Find a class with a predicate.
     *
     * @param predicate A predicate to match the class.
     * @return An immutable instance of the class type.
     * @see mutableClassBy
     */
    fun classBy(predicate: (ClassDef) -> Boolean) = classes.classBy(predicate)

    /**
     * Find a class with a predicate.
     *
     * @param classType The full classname.
     * @return An immutable instance of the class type.
     * @see mutableClassBy
     */
    fun classByOrNull(classType: String) = classes.classByOrNull(classType)

    /**
     * Find a class with a predicate.
     *
     * @param predicate A predicate to match the class.
     * @return An immutable instance of the class type.
     */
    fun classByOrNull(predicate: (ClassDef) -> Boolean) = classes.classByOrNull(predicate)

    /**
     * Find a class with a predicate.
     *
     * @param classType The full classname.
     * @return A mutable version of the class type.
     */
    fun mutableClassBy(classType: String) = classes.mutableClassBy(classType)

    /**
     * Find a class with a predicate.
     *
     * @param classDef An immutable class.
     * @return A mutable version of the class definition.
     */
    fun mutableClassBy(classDef: ClassDef) = classes.mutableClassBy(classDef)

    /**
     * Find a class with a predicate.
     *
     * @param predicate A predicate to match the class.
     * @return A mutable class that matches the predicate.
     */
    fun mutableClassBy(predicate: (ClassDef) -> Boolean) = classes.mutableClassBy(predicate)

    /**
     * Mutable class from a full class name.
     * Returns `null` if class is not available, such as a built in Android or Java library.
     *
     * @param classType The full classname.
     * @return A mutable version of the class type.
     */
    fun mutableClassByOrNull(classType: String) = classes.mutableClassByOrNull(classType)

    /**
     * Find a mutable class with a predicate.
     *
     * @param predicate A predicate to match the class.
     * @return A mutable class that matches the predicate.
     */
    fun mutableClassByOrNull(predicate: (ClassDef) -> Boolean) = classes.mutableClassByOrNull(predicate)

    /**
     * @return The mutable instance of an immutable class.
     */
    @Deprecated("Instead use `mutableClassBy(String)`, `mutableClassBy(ClassDef)`, or `mutableClassBy(predicate)`")
    fun proxy(classDef: ClassDef) = classes.mutableClassBy(classDef)

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
                        override fun getClasses() = this@BytecodePatchContext.classes.pool.values.toSet()

                        override fun getOpcodes() = this@BytecodePatchContext.opcodes
                    },
                    DexIO.DEFAULT_MAX_DEX_POOL_SIZE,
                ) { _, entryName, _ -> logger.info { "Compiled $entryName" } }
            }.listFiles(FileFilter { it.isFile })!!.map {
                PatcherResult.PatchedDexFile(it.name, it.inputStream())
            }.toSet()

        System.gc()

        return patchedDexFileResults
    }

    /**
     * A lookup map for strings and the methods they are a member of.
     *
     * @param classes The list of classes to create the lookup maps from.
     */
    internal class LookupMaps(classes: Collection<ClassDef>) : Closeable {
        /**
         * Methods associated by strings referenced in them.
         */
        private val methodsByStrings = HashMap<String, LinkedList<MethodClassPair>>()

        /**
         * All methods that contain at least 1 string.
         */
        internal val allMethodsWithStrings by lazy {
            methodsByStrings.toSortedMap().values
                .flatMap { it }
                .distinctBy { it.first }
        }

        init {
            classes.forEach { classDef ->
                classDef.methods.forEach { method ->
                    val methodClassPair: MethodClassPair by lazy {
                        method to classDef
                    }

                    // Add strings contained in the method as the key.
                    method.instructionsOrNull?.forEach { instruction ->
                        val opcode = instruction.opcode
                        if (opcode != Opcode.CONST_STRING && opcode != Opcode.CONST_STRING_JUMBO) {
                            return@forEach
                        }

                        val string = ((instruction as ReferenceInstruction).reference as StringReference).string
                        methodsByStrings.getOrPut(string) { LinkedList() }.add(methodClassPair)
                    }
                }
            }
        }

        fun getMethodClassPairsForString(stringLiteral: String): List<MethodClassPair>? {
            return methodsByStrings[stringLiteral]
        }

        override fun close() {
            methodsByStrings.clear()
        }
    }

    override fun close() {
        lookupMaps.close()
        classes.close()
    }
}

/**
 * A pair of a [Method] and the [ClassDef] it is a member of.
 */
internal typealias MethodClassPair = Pair<Method, ClassDef>

