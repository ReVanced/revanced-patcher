package app.revanced.patcher.patch

import app.revanced.patcher.InternalApi
import app.revanced.patcher.PatcherConfig
import app.revanced.patcher.PatcherResult
import app.revanced.patcher.dex.mutable.MutableClassDef
import app.revanced.patcher.dex.mutable.MutableClassDef.Companion.toMutable
import app.revanced.patcher.extensions.instructionsOrNull
import app.revanced.patcher.util.ClassMerger.merge
import app.revanced.patcher.util.MethodNavigator
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
import java.io.IOException
import java.util.LinkedHashMap
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
    internal val lookupMaps by lazy { _lookupMaps ?: LookupMaps().also { _lookupMaps = it } }
    private var _lookupMaps: LookupMaps? = null // For freeing up memory when compiling.

    /**
     * Merge the extension of [bytecodePatch] into the [BytecodePatchContext].
     * If no extension is present, the function will return early.
     *
     * @param bytecodePatch The [BytecodePatch] to merge the extension of.
     */
    internal fun mergeExtension(bytecodePatch: BytecodePatch) {
        bytecodePatch.extensionInputStream?.get()?.use { extensionStream ->
            RawDexIO.readRawDexFile(extensionStream, 0, null).classes.forEach { classDef ->
                val existingClass = lookupMaps.classDefsByType[classDef.type] ?: run {
                    logger.fine { "Adding class \"$classDef\"" }

                    classDefs += classDef
                    lookupMaps += classDef

                    return@forEach
                }

                logger.fine { "Class \"$classDef\" exists already. Adding missing methods and fields." }

                existingClass.merge(classDef, this@BytecodePatchContext).let { mergedClass ->
                    // If the class was merged, replace the original class with the merged class.
                    if (mergedClass === existingClass) {
                        return@let
                    }

                    classDefs -= existingClass
                    lookupMaps -= existingClass

                    classDefs += mergedClass
                    lookupMaps += mergedClass
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
    fun ClassDef.mutable(): MutableClassDef = this as? MutableClassDef ?: also {
        classDefs -= this
        lookupMaps -= this
    }.toMutable().also {
        classDefs += it
        lookupMaps += it
    }

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
        close()
        System.gc()

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
                        override fun getClasses() = this@BytecodePatchContext.classDefs.let {
                            // More performant according to
                            // https://github.com/LisoUseInAIKyrios/revanced-patcher/commit/8c26ad08457fb1565ea5794b7930da42a1c81cf1#diff-be698366d9868784ecf7da3fd4ac9d2b335b0bb637f9f618fbe067dbd6830b8fR197
                            // TODO: Benchmark, if actually faster.
                            HashSet<ClassDef>(it.size * 3 / 2).apply { addAll(it) }
                        }

                        override fun getOpcodes() = this@BytecodePatchContext.opcodes
                    },
                    DexIO.DEFAULT_MAX_DEX_POOL_SIZE,
                ) { _, entryName, _ -> logger.info { "Compiled $entryName" } }
            }.listFiles { it.isFile }!!.map {
                PatcherResult.PatchedDexFile(it.name, it.inputStream())
            }.toSet()

        // Free up more memory, although it is unclear if this is actually helpful.
        classDefs.clear()
        System.gc()

        return patchedDexFileResults
    }

    override fun close() {
        try {
            _lookupMaps = null
        } catch (e: IOException) {
            logger.warning("Failed to clear BytecodePatchContext: ${e.message}")
        }
    }

    internal inner class LookupMaps {
        // No custom HashMap needed here, according to
        // https://github.com/LisoUseInAIKyrios/revanced-patcher/commit/9b6d95d4f414a35ed68da37b0ecd8549df1ef63a
        // TODO: Benchmark, if actually faster.
        private val _classDefsByType = mutableMapOf<String, ClassDef>()
        val classDefsByType: Map<String, ClassDef> = _classDefsByType

        // Better performance according to
        // https://github.com/LisoUseInAIKyrios/revanced-patcher/commit/9b6d95d4f414a35ed68da37b0ecd8549df1ef63a
        private val _methodsByStrings =
            LinkedHashMap<String, MutableList<Method>>(2 * classDefs.size, 0.5f)

        val methodsByStrings: Map<String, List<Method>> = _methodsByStrings

        private val _methodsWithString = methodsByStrings.values.flatten().toMutableSet()
        val methodsWithString: Set<Method> = _methodsWithString

        init {
            classDefs.forEach(::plusAssign)
        }

        private fun ClassDef.forEachString(action: (Method, String) -> Unit) = methods.asSequence().forEach { method ->
            method.instructionsOrNull?.asSequence()
                ?.filterIsInstance<ReferenceInstruction>()
                ?.map { it.reference }
                ?.filterIsInstance<StringReference>()
                ?.map { it.string }
                ?.forEach { string ->
                    action(method, string)
                }
        }

        operator fun plusAssign(classDef: ClassDef) {
            _classDefsByType[classDef.type] = classDef

            classDef.forEachString { method, string ->
                _methodsWithString += method
                _methodsByStrings.getOrPut(string) {
                    mutableListOf()
                } += method
            }
        }

        operator fun minusAssign(classDef: ClassDef) {
            _classDefsByType -= classDef.type

            classDef.forEachString { method, string ->
                _methodsWithString.remove(method)

                if (_methodsByStrings[string]?.also { it -= method }?.isEmpty() == true)
                    _methodsByStrings -= string
            }
        }
    }
}
