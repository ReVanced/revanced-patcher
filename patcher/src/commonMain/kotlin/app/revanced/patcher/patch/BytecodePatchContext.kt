package app.revanced.patcher.patch

import app.revanced.com.android.tools.smali.dexlib2.mutable.MutableClassDef
import app.revanced.com.android.tools.smali.dexlib2.mutable.MutableClassDef.Companion.toMutable
import app.revanced.java.io.kmpDeleteRecursively
import app.revanced.java.io.kmpInputStream
import app.revanced.java.io.kmpResolve
import app.revanced.patcher.PatchesResult
import app.revanced.patcher.extensions.instructionsOrNull
import app.revanced.patcher.extensions.string
import app.revanced.patcher.util.ClassMerger.merge
import app.revanced.patcher.util.MethodNavigator
import app.revanced.patcher.util.proxy.ClassProxy
import com.android.tools.smali.dexlib2.Opcodes
import com.android.tools.smali.dexlib2.dexbacked.DexBackedDexFile
import com.android.tools.smali.dexlib2.iface.ClassDef
import com.android.tools.smali.dexlib2.iface.DexFile
import com.android.tools.smali.dexlib2.iface.Method
import com.android.tools.smali.dexlib2.iface.MultiDexContainer
import com.android.tools.smali.dexlib2.iface.reference.MethodReference
import lanchon.multidexlib2.BasicDexFileNamer
import lanchon.multidexlib2.DexIO
import lanchon.multidexlib2.DuplicateTypeException
import lanchon.multidexlib2.MultiDexIO
import lanchon.multidexlib2.OpcodeUtils
import lanchon.multidexlib2.RawDexIO
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.io.InputStream
import java.util.logging.Logger
import kotlin.reflect.jvm.jvmName

/**
 * A context for patches containing the current state of the bytecode.
 *
 * @param apkFile The apk [File] to patch.
 * @param patchedFilesPath The path to the temporary apk files directory.
 */
@Suppress("MemberVisibilityCanBePrivate")
class BytecodePatchContext internal constructor(
    internal val apkFile: File,
    internal val patchedFilesPath: File,
) : PatchContext<Set<PatchesResult.PatchedDexFile>> {
    private val logger = Logger.getLogger(this::class.jvmName)

    inner class ClassDefs private constructor(
        /**
         * The dex files of the apk in the order they were read in.
         */
        private val dexFiles: List<DexFile>,
        /**
         * The index of the dex file each class that no patch modified originates from,
         * mapped by the type of the class.
         */
        private val dexFileIndexByType: MutableMap<String, Int>,
        internal val opcodes: Opcodes,
        private val classDefs: MutableSet<ClassDef>,
    ) : MutableSet<ClassDef> by classDefs {
        private val byType = mutableMapOf<String, ClassDef>()

        operator fun get(name: String): ClassDef? = byType[name]

        // Better performance according to
        // https://github.com/LisoUseInAIKyrios/revanced-patcher/commit/9b6d95d4f414a35ed68da37b0ecd8549df1ef63a
        private val _methodsByStrings =
            LinkedHashMap<String, MutableSet<Method>>(2 * size, 0.5f)

        val methodsByString: Map<String, Set<Method>> = _methodsByStrings

        // Can have a use-case in the future:
        // private val _methodsWithString = methodsByString.values.flatten().toMutableSet()
        // val methodsWithString: Set<Method> = _methodsWithString

        /**
         * The indices of the dex files of which at least one class was modified.
         */
        private val modifiedDexFileIndices = mutableSetOf<Int>()

        constructor() : this(ApkDexFiles(apkFile))

        private constructor(apkDexFiles: ApkDexFiles) : this(
            apkDexFiles.dexFiles,
            apkDexFiles.dexFileIndexByType,
            apkDexFiles.opcodes,
            apkDexFiles.classDefs,
        )

        override fun add(element: ClassDef): Boolean {
            val added = classDefs.add(element)
            if (added) addCache(element)

            return added
        }

        override fun addAll(elements: Collection<ClassDef>): Boolean {
            var anyAdded = false
            elements.forEach { element ->
                val added = classDefs.add(element)
                if (added) {
                    addCache(element)
                    anyAdded = true
                }
            }

            return anyAdded
        }

        // TODO: There is one default method "removeIf" in MutableSet, which we cannot override in the common module.
        //  The method must be overloaded with a NotImplementedException to avoid cache desynchronization.

        override fun clear() {
            classDefs.clear()
            byType.clear()
            _methodsByStrings.clear()

            modifiedDexFileIndices += dexFileIndexByType.values
            dexFileIndexByType.clear()
        }

        override fun remove(element: ClassDef): Boolean {
            val removed = classDefs.remove(element)
            if (removed) {
                removeCache(element)
                markModified(element)
            }

            return removed
        }

        override fun removeAll(elements: Collection<ClassDef>): Boolean {
            var anyRemoved = false
            elements.forEach { element ->
                val removed = classDefs.remove(element)
                if (removed) {
                    removeCache(element)
                    markModified(element)
                    anyRemoved = true
                }
            }

            return anyRemoved
        }

        override fun retainAll(elements: Collection<ClassDef>) =
            removeAll(classDefs.asSequence().filter { it !in elements })

        /**
         * Mark the dex file the [classDef] originates from as modified.
         *
         * @param classDef The [ClassDef] that was removed from or replaced in this set.
         */
        private fun markModified(classDef: ClassDef) {
            dexFileIndexByType.remove(classDef.type)?.let { modifiedDexFileIndices += it }
        }

        private fun addCache(classDef: ClassDef) {
            byType[classDef.type] = classDef

            classDef.forEachString { method, string ->
                _methodsByStrings.getOrPut(string) {
                    // Maybe adjusting load factor/ initial size can improve performance.
                    mutableSetOf()
                } += method
            }
        }

        private fun removeCache(classDef: ClassDef) {
            byType -= classDef.type

            classDef.forEachString { method, string ->
                if (_methodsByStrings[string]?.also { it -= method }?.isEmpty() == true) {
                    _methodsByStrings -= string
                }
            }
        }

        private fun ClassDef.forEachString(action: (Method, String) -> Unit) {
            methods.asSequence().forEach { method ->
                method.instructionsOrNull
                    ?.asSequence()
                    ?.mapNotNull { it.string }
                    ?.forEach { string -> action(method, string) }
            }
        }

        /**
         * Create a mutable version of an existing class by the type of the given [classDef], replacing it in the set if necessary.
         *
         * @param classDef The [ClassDef] to get or replace.
         * @return The mutable version of the [classDef].
         * @see MutableClassDef
         * @see toMutable
         */
        fun getOrReplaceMutable(classDef: ClassDef): MutableClassDef {
            val currentClassDef = get(classDef.type)!!

            if (currentClassDef !is MutableClassDef) {
                val mutableClassDef = currentClassDef.toMutable()
                this -= classDef
                this += mutableClassDef

                return mutableClassDef
            }

            return currentClassDef
        }

        /**
         * Split the classes into the dex files that can be reused as-is and the classes that have to be compiled.
         *
         * A dex file can be reused as-is if no patch added, removed or replaced any of the classes it contains.
         * Because a class cannot be moved out of the dex file it is compiled into,
         * every class of a modified dex file has to be compiled again.
         *
         * This invalidates [dexFileIndexByType], so it must only be called once, when compiling.
         *
         * @return The unmodified dex files in the order they were read in, and the classes to compile.
         */
        internal fun partitionForCompilation(): Pair<List<DexFile>, Set<ClassDef>> {
            val unmodifiedDexFileIndices =
                dexFiles.indices.filterTo(mutableSetOf()) { index ->
                    index !in modifiedDexFileIndices && dexFiles[index].canBeReused
                }

            // More performant according to
            // https://github.com/LisoUseInAIKyrios/revanced-patcher/
            // commit/8c26ad08457fb1565ea5794b7930da42a1c81cf1
            // #diff-be698366d9868784ecf7da3fd4ac9d2b335b0bb637f9f618fbe067dbd6830b8fR197
            // TODO: Benchmark, if actually faster.
            val classDefsToCompile = HashSet<ClassDef>(classDefs.size * 3 / 2)
            classDefs.forEach { classDef ->
                val index = dexFileIndexByType[classDef.type]
                if (index == null || index !in unmodifiedDexFileIndices) classDefsToCompile += classDef
            }

            dexFileIndexByType.clear()

            return unmodifiedDexFileIndices.sorted().map(dexFiles::get) to classDefsToCompile
        }

        internal fun initializeCache() = classDefs.forEach(::addCache)

        internal fun clearCache() {
            byType.clear()
            _methodsByStrings.clear()
        }
    }

    /**
     * The list of classes.
     */
    val classDefs = ClassDefs()

    @Deprecated("Use classDefs instead")
    val classes = classDefs

    @Deprecated("Use classDefs.firstOrNull instead")
    fun classBy(predicate: (ClassDef) -> Boolean) =
        classDefs.firstOrNull { predicate(it) }?.let {
            ClassProxy(classDefs.getOrReplaceMutable(it))
        }

    @Deprecated(
        "Use classDefs.getOrReplaceMutable instead",
        ReplaceWith("classDefs.getOrReplaceMutable(classDef)")
    )
    fun proxy(classDef: ClassDef) = ClassProxy(classDefs.getOrReplaceMutable(classDef))

    /**
     * Add classes from  [extensionInputStream] to this [BytecodePatchContext].
     *
     * @param extensionInputStream The input stream for an extension dex file.
     */
    internal fun addExtension(extensionInputStream: InputStream) {
        RawDexIO.readRawDexFile(extensionInputStream, 0, null).classes.forEach { classDef ->
            val existingClass =
                classDefs[classDef.type] ?: run {
                    logger.fine { "Adding class \"$classDef\"" }

                    classDefs += classDef

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

        extensionInputStream.close()
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
    override fun get(): Set<PatchesResult.PatchedDexFile> {
        val (unmodifiedDexFiles, classDefsToCompile) = classDefs.partitionForCompilation()

        classDefs.clearCache()
        System.gc()

        val namer = BasicDexFileNamer()

        val dexFilesPath =
            patchedFilesPath
                .kmpResolve("dex")
                .also {
                    it.kmpDeleteRecursively() // Make sure the directory is empty.
                    it.mkdirs()
                }

        val compiledDexFilesPath =
            patchedFilesPath
                .kmpResolve("compiled-dex")
                .also {
                    it.kmpDeleteRecursively() // Make sure the directory is empty.
                    it.mkdirs()
                }

        logger.info {
            "Compiling ${classDefsToCompile.size} classes into dex files, " +
                "reusing ${unmodifiedDexFiles.size} unmodified dex files"
        }

        // Compiled to their own directory, because the unmodified dex files
        // are named after the dex files compiled here.
        if (classDefsToCompile.isNotEmpty()) {
            MultiDexIO.writeDexFile(
                true,
                -1,
                compiledDexFilesPath,
                namer,
                object : DexFile {
                    override fun getClasses() = classDefsToCompile

                    override fun getOpcodes() = classDefs.opcodes
                },
                DexIO.DEFAULT_MAX_DEX_POOL_SIZE,
            ) { _, entryName, _ -> logger.info { "Compiled $entryName" } }
        }

        // Android stops loading dex files at the first name it does not find,
        // so the dex files must be named without gaps in their numbering.
        var dexFileIndex = 0
        fun nextDexFile() = dexFilesPath.kmpResolve(namer.getName(dexFileIndex++))

        val patchedDexFiles =
            unmodifiedDexFiles.map { dexFile ->
                nextDexFile().also { dexFile.writeTo(it) }
            } +
                compiledDexFilesPath
                    .listFiles { it.isFile }!!
                    .sortedBy { namer.getIndex(it.name) }
                    .map { compiledDexFile ->
                        nextDexFile().also {
                            if (!compiledDexFile.renameTo(it)) {
                                throw IOException("Failed to move \"$compiledDexFile\" to \"$it\"")
                            }
                        }
                    }

        compiledDexFilesPath.kmpDeleteRecursively()

        return patchedDexFiles
            .map {
                PatchesResult.PatchedDexFile(it.name, it.kmpInputStream())
            }.toSet()
    }
}

/**
 * The bytes the dex file was read from, or null if the dex file was not read from a buffer of its own.
 */
private val DexFile.rawBytes
    get() = (this as? DexBackedDexFile)?.buffer?.takeIf { it.baseOffset == 0 }?.buf

/**
 * Whether the dex file can be reused as-is instead of being compiled again.
 */
private val DexFile.canBeReused
    get() = rawBytes != null

/**
 * Write the dex file to the [file], copying it verbatim if possible.
 *
 * @param file The [File] to write the dex file to.
 */
private fun DexFile.writeTo(file: File) =
    rawBytes?.let { bytes ->
        FileOutputStream(file).use { it.write(bytes) }
    } ?: RawDexIO.writeRawDexFile(file, this, DexIO.DEFAULT_MAX_DEX_POOL_SIZE)

/**
 * The dex files of an apk and the classes they contain.
 *
 * @param apkFile The apk [File] to read the dex files of.
 */
private class ApkDexFiles(
    apkFile: File,
) {
    val dexFiles: List<DexFile>
    val classDefs = LinkedHashSet<ClassDef>()
    val dexFileIndexByType = mutableMapOf<String, Int>()
    val opcodes: Opcodes

    init {
        val container: MultiDexContainer<out DexFile> =
            MultiDexIO.readMultiDexContainer(
                true,
                apkFile,
                BasicDexFileNamer(),
                null,
                null,
            )

        dexFiles = container.dexEntryNames.map { entryName -> container.getEntry(entryName)!!.dexFile }

        var opcodes: Opcodes? = null

        dexFiles.forEachIndexed { index, dexFile ->
            dexFile.classes.forEach { classDef ->
                if (!classDefs.add(classDef)) throw DuplicateTypeException(classDef.type)

                dexFileIndexByType[classDef.type] = index
            }

            opcodes = OpcodeUtils.getNewestOpcodes(opcodes, dexFile.opcodes, true)
        }

        this.opcodes = opcodes!!
    }
}
