package app.revanced.patcher.patch

import app.revanced.com.android.tools.smali.dexlib2.ReadResult
import app.revanced.com.android.tools.smali.dexlib2.mutable.MutableClassDef
import app.revanced.com.android.tools.smali.dexlib2.mutable.MutableMethod
import app.revanced.com.android.tools.smali.dexlib2.nullingArrayIteratorOf
import app.revanced.com.android.tools.smali.dexlib2.readMultiDex
import app.revanced.com.android.tools.smali.dexlib2.writeMultiDex
import app.revanced.java.io.kmpDeleteRecursively
import app.revanced.java.io.kmpInputStream
import app.revanced.java.io.kmpResolve
import app.revanced.patcher.PatchesResult
import app.revanced.patcher.extensions.instructionsOrNull
import app.revanced.patcher.extensions.string
import app.revanced.patcher.util.ClassMerger.merge
import app.revanced.patcher.util.MethodNavigator
import app.revanced.patcher.util.proxy.ClassProxy
import com.android.tools.smali.dexlib2.dexbacked.DexBackedDexFile
import com.android.tools.smali.dexlib2.iface.ClassDef
import com.android.tools.smali.dexlib2.iface.Method
import com.android.tools.smali.dexlib2.iface.reference.MethodReference
import com.android.tools.smali.dexlib2.util.DexUtil
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
        readResult: ReadResult,
        private val classDefs: MutableSet<ClassDef> = readResult.classDefs,
    ) : MutableSet<ClassDef> by classDefs {
        private val byType = HashMap<String, ClassDef>(size)

        operator fun get(type: String): ClassDef? = byType[type]

        // Assuming that each class has at least 2 unique strings.
        private val _methodsByStrings = HashMap<String, MutableSet<Method>>(2 * size)

        val methodsByString: Map<String, Set<Method>> = _methodsByStrings

        // Can have a use-case in the future:
        // private val _methodsWithString = methodsByString.values.flatten().toMutableSet()
        // val methodsWithString: Set<Method> = _methodsWithString

        constructor() : this(readMultiDex(apkFile))

        internal val opcodes = readResult.opcodes

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

        override fun retainAll(elements: Collection<ClassDef>) =
            removeAll(classDefs.asSequence().filter { it !in elements })

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
         * Create a mutable instance of an existing class with the type of the given [classDef],
         * replacing it in the set if necessary.
         *
         * @param classDef The [ClassDef] to get or replace.
         * @return The mutable version of the [classDef].
         * @see MutableClassDef
         */
        @Deprecated("Use makeMutable(ClassDef) instead", ReplaceWith("makeMutable(classDef)"))
        fun getOrReplaceMutable(classDef: ClassDef) = makeMutable(classDef)

        /**
         * Create a mutable instance of an existing class with the type of the given [classDef],
         * replacing it in the set if necessary.
         *
         * @param classDef The [ClassDef] to get or replace.
         * @return The mutable instance.
         * @see MutableClassDef
         */
        fun makeMutable(classDef: ClassDef): MutableClassDef {
            val currentClassDef = get(classDef.type)!!

            if (classDef !== currentClassDef) throw IllegalStateException(
                "The instance of the given classDef is not the same as the instance in the set. " +
                        "This can lead to desynchronization. Use the instance from the set instead."
            )

            if (currentClassDef is MutableClassDef) return currentClassDef

            val mutableClassDef = MutableClassDef(currentClassDef)
            this -= currentClassDef
            this += mutableClassDef

            return mutableClassDef
        }

        /**
         * Create a mutable instance of an existing method, replacing it in the class if necessary.
         *
         * @param method The [Method] to get or replace.
         * @return The mutable instance.
         * @see MutableMethod
         */
        fun makeMutable(method: Method): MutableMethod {
            val classDef = get(method.definingClass)!!
            if (classDef.methods.none { method === it }) throw IllegalArgumentException(
                "The instance of this method is not present in the class ${classDef.type} in the context."
            )

            return makeMutable(classDef).methods.first { it == method }
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
        val extensionBytes =
            extensionInputStream.readBytes().also { DexUtil.verifyDexHeader(it, 0); }

        DexBackedDexFile(null, extensionBytes, 0).classes.forEach { classDef ->
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

        val patchedDexFilesPath = patchedFilesPath
            .kmpResolve("dex")
            .also {
                it.kmpDeleteRecursively() // Make sure the directory is empty.
                it.mkdirs()
            }

        val classDefsIterator = nullingArrayIteratorOf(classDefs)

        classDefs.clear() // Make it possible to GC written classes while writing dex files.

        writeMultiDex(
            patchedDexFilesPath,
            classDefsIterator,
            classDefs.opcodes,
            -1
        ) { index, _ ->
            logger.info { "Compiled classes$index.dex" }
        }

        return patchedDexFilesPath.listFiles { it.isFile }!!
            .map { PatchesResult.PatchedDexFile(it.name, it.kmpInputStream()) }.toSet()
    }
}
