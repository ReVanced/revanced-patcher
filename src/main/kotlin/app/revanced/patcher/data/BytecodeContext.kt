package app.revanced.patcher.data

import app.revanced.patcher.PatcherContext
import app.revanced.patcher.PatcherOptions
import app.revanced.patcher.PatcherResult
import app.revanced.patcher.patch.Patch
import app.revanced.patcher.util.ClassMerger.merge
import app.revanced.patcher.util.ProxyClassList
import app.revanced.patcher.util.method.MethodWalker
import app.revanced.patcher.util.proxy.ClassProxy
import com.android.tools.smali.dexlib2.Opcodes
import com.android.tools.smali.dexlib2.iface.ClassDef
import com.android.tools.smali.dexlib2.iface.DexFile
import com.android.tools.smali.dexlib2.iface.Method
import lanchon.multidexlib2.BasicDexFileNamer
import lanchon.multidexlib2.DexIO
import lanchon.multidexlib2.MultiDexIO
import java.io.File
import java.io.FileFilter
import java.io.Flushable
import java.util.logging.Logger

/**
 * A context for bytecode.
 * This holds the current state of the bytecode.
 *
 * @param options The [PatcherOptions] used to create this context.
 */
class BytecodeContext internal constructor(private val options: PatcherOptions) :
    Context<List<PatcherResult.PatchedDexFile>> {
        private val logger = Logger.getLogger(BytecodeContext::class.java.name)

        /**
         * [Opcodes] of the supplied [PatcherOptions.inputFile].
         */
        internal lateinit var opcodes: Opcodes

        /**
         * The list of classes.
         */
        val classes by lazy {
            ProxyClassList(
                MultiDexIO.readDexFile(
                    true,
                    options.inputFile,
                    BasicDexFileNamer(),
                    null,
                    null,
                ).also { opcodes = it.opcodes }.classes.toMutableSet(),
            )
        }

        /**
         * The [Integrations] of this [PatcherContext].
         */
        internal val integrations = Integrations()

        /**
         * Find a class by a given class name.
         *
         * @param className The name of the class.
         * @return A proxy for the first class that matches the class name.
         */
        fun findClass(className: String) = findClass { it.type.contains(className) }

        /**
         * Find a class by a given predicate.
         *
         * @param predicate A predicate to match the class.
         * @return A proxy for the first class that matches the predicate.
         */
        fun findClass(predicate: (ClassDef) -> Boolean) =
            // if we already proxied the class matching the predicate...
            classes.proxies.firstOrNull { predicate(it.immutableClass) }
                ?: // else resolve the class to a proxy and return it, if the predicate is matching a class
                classes.find(predicate)?.let { proxy(it) }

        /**
         * Proxy a class.
         * This will allow the class to be modified.
         *
         * @param classDef The class to proxy.
         * @return A proxy for the class.
         */
        fun proxy(classDef: ClassDef) =
            this.classes.proxies.find { it.immutableClass.type == classDef.type } ?: let {
                ClassProxy(classDef).also { this.classes.add(it) }
            }

        /**
         * Create a [MethodWalker] instance for the current [BytecodeContext].
         *
         * @param startMethod The method to start at.
         * @return A [MethodWalker] instance.
         */
        fun toMethodWalker(startMethod: Method) = MethodWalker(this, startMethod)

        /**
         * Compile bytecode from the [BytecodeContext].
         *
         * @return The compiled bytecode.
         */
        override fun get(): List<PatcherResult.PatchedDexFile> {
            logger.info("Compiling patched dex files")

            val patchedDexFileResults =
                options.resourceCachePath.resolve("dex").also {
                    it.deleteRecursively() // Make sure the directory is empty.
                    it.mkdirs()
                }.apply {
                    MultiDexIO.writeDexFile(
                        true,
                        if (options.multithreadingDexFileWriter) -1 else 1,
                        this,
                        BasicDexFileNamer(),
                        object : DexFile {
                            override fun getClasses() = this@BytecodeContext.classes.also(ProxyClassList::replaceClasses)

                            override fun getOpcodes() = this@BytecodeContext.opcodes
                        },
                        DexIO.DEFAULT_MAX_DEX_POOL_SIZE,
                    ) { _, entryName, _ -> logger.info("Compiled $entryName") }
                }.listFiles(FileFilter { it.isFile })!!.map { PatcherResult.PatchedDexFile(it.name, it.inputStream()) }

            System.gc()

            return patchedDexFileResults
        }

        /**
         * The integrations of a [PatcherContext].
         */
        internal inner class Integrations : MutableList<File> by mutableListOf(), Flushable {
            /**
             * Whether to merge integrations.
             * Set to true, if the field requiresIntegrations of any supplied [Patch] is true.
             */
            var merge = false

            /**
             * Merge integrations into the [BytecodeContext] and flush all [Integrations].
             */
            override fun flush() {
                if (!merge) return

                logger.info("Merging integrations")

                val classMap = classes.associateBy { it.type }

                this@Integrations.forEach { integrations ->
                    MultiDexIO.readDexFile(
                        true,
                        integrations,
                        BasicDexFileNamer(),
                        null,
                        null,
                    ).classes.forEach classDef@{ classDef ->
                        val existingClass =
                            classMap[classDef.type] ?: run {
                                logger.fine("Adding $classDef")
                                classes.add(classDef)
                                return@classDef
                            }

                        logger.fine("$classDef exists. Adding missing methods and fields.")

                        existingClass.merge(classDef, this@BytecodeContext).let { mergedClass ->
                            // If the class was merged, replace the original class with the merged class.
                            if (mergedClass === existingClass) return@let
                            classes.apply {
                                remove(existingClass)
                                add(mergedClass)
                            }
                        }
                    }
                }
                clear()
            }
        }
    }
