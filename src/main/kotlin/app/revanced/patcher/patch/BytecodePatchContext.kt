package app.revanced.patcher.patch

import app.revanced.patcher.InternalApi
import app.revanced.patcher.PatcherConfig
import app.revanced.patcher.PatcherContext
import app.revanced.patcher.PatcherResult
import app.revanced.patcher.util.ClassMerger.merge
import app.revanced.patcher.util.MethodNavigator
import app.revanced.patcher.util.ProxyClassList
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
 * A context for patches containing the current state of the bytecode.
 *
 * @param config The [PatcherConfig] used to create this context.
 */
@Suppress("MemberVisibilityCanBePrivate")
class BytecodePatchContext internal constructor(private val config: PatcherConfig) :
    PatchContext<Set<PatcherResult.PatchedDexFile>> {
    private val logger = Logger.getLogger(BytecodePatchContext::class.java.name)

    /**
     * [Opcodes] of the supplied [PatcherConfig.apkFile].
     */
    internal lateinit var opcodes: Opcodes

    /**
     * The list of classes.
     */
    val classes by lazy {
        ProxyClassList(
            MultiDexIO.readDexFile(
                true,
                config.apkFile,
                BasicDexFileNamer(),
                null,
                null,
            ).also { opcodes = it.opcodes }.classes.toMutableList(),
        )
    }

    /**
     * The [Integrations] of this [PatcherContext].
     */
    internal val integrations = Integrations()

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
    fun navigator(method: Method) = MethodNavigator(this@BytecodePatchContext, method)

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
     * The integrations of a [PatcherContext].
     */
    internal inner class Integrations : MutableList<File> by mutableListOf(), Flushable {
        /**
         * Whether to merge integrations.
         * Set to true, if the field requiresIntegrations of any supplied [Patch] is true.
         */
        var merge = false

        /**
         * Merge integrations into the [BytecodePatchContext] and flush all [Integrations].
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

                    existingClass.merge(classDef, this@BytecodePatchContext).let { mergedClass ->
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
