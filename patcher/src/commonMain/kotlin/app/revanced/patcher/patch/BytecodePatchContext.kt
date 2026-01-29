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
import com.android.tools.smali.dexlib2.iface.ClassDef
import com.android.tools.smali.dexlib2.iface.DexFile
import com.android.tools.smali.dexlib2.iface.Method
import com.android.tools.smali.dexlib2.iface.reference.MethodReference
import lanchon.multidexlib2.BasicDexFileNamer
import lanchon.multidexlib2.DexIO
import lanchon.multidexlib2.MultiDexIO
import lanchon.multidexlib2.RawDexIO
import java.io.File
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
        dexFile: DexFile,
        private val classDefs: MutableSet<ClassDef> = dexFile.classes.toMutableSet(),
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

        constructor() : this(
            MultiDexIO.readDexFile(
                true,
                apkFile,
                BasicDexFileNamer(),
                null,
                null,
            ),
        )

        internal val opcodes = dexFile.opcodes

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
        }

        override fun remove(element: ClassDef): Boolean {
            val removed = classDefs.remove(element)
            if (removed) removeCache(element)

            return removed
        }

        override fun removeAll(elements: Collection<ClassDef>): Boolean {
            var anyRemoved = false
            elements.forEach { element ->
                val removed = classDefs.remove(element)
                if (removed) {
                    removeCache(element)
                    anyRemoved = true
                }
            }

            return anyRemoved
        }

        override fun retainAll(elements: Collection<ClassDef>) = removeAll(classDefs.asSequence().filter { it !in elements })

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
         * Get a mutable version of the given [classDef], replacing it in the set if necessary.
         *
         * @param classDef The [ClassDef] to get or replace.
         * @return The mutable version of the [classDef].
         * @see MutableClassDef
         * @see toMutable
         */
        fun getOrReplaceMutable(classDef: ClassDef): MutableClassDef {
            if (classDef !is MutableClassDef) {
                val mutableClassDef = classDef.toMutable()
                this -= classDef
                this += mutableClassDef

                return mutableClassDef
            }

            return classDef
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
        logger.info("Compiling patched dex files")

        classDefs.clearCache()
        System.gc()

        val patchedDexFileResults =
            patchedFilesPath
                .kmpResolve("dex")
                .also {
                    it.kmpDeleteRecursively() // Make sure the directory is empty.
                    it.mkdirs()
                }.apply {
                    MultiDexIO.writeDexFile(
                        true,
                        -1,
                        this,
                        BasicDexFileNamer(),
                        object : DexFile {
                            override fun getClasses() =
                                classDefs.let {
                                    // More performant according to
                                    // https://github.com/LisoUseInAIKyrios/revanced-patcher/
                                    // commit/8c26ad08457fb1565ea5794b7930da42a1c81cf1
                                    // #diff-be698366d9868784ecf7da3fd4ac9d2b335b0bb637f9f618fbe067dbd6830b8fR197
                                    // TODO: Benchmark, if actually faster.
                                    HashSet<ClassDef>(it.size * 3 / 2).apply { addAll(it) }
                                }

                            override fun getOpcodes() = classDefs.opcodes
                        },
                        DexIO.DEFAULT_MAX_DEX_POOL_SIZE,
                    ) { _, entryName, _ -> logger.info { "Compiled $entryName" } }
                }.listFiles { it.isFile }!!
                .map {
                    PatchesResult.PatchedDexFile(it.name, it.kmpInputStream())
                }.toSet()

        return patchedDexFileResults
    }
}
