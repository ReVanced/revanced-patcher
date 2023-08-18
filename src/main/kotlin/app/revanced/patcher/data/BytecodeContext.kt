package app.revanced.patcher.data

import app.revanced.patcher.PatcherOptions
import app.revanced.patcher.PatcherResult
import app.revanced.patcher.logging.Logger
import app.revanced.patcher.util.ClassMerger.merge
import app.revanced.patcher.util.ProxyClassList
import app.revanced.patcher.util.method.MethodWalker
import app.revanced.patcher.util.proxy.ClassProxy
import com.android.tools.smali.dexlib2.Opcodes
import com.android.tools.smali.dexlib2.iface.ClassDef
import com.android.tools.smali.dexlib2.iface.DexFile
import com.android.tools.smali.dexlib2.iface.Method
import com.android.tools.smali.dexlib2.writer.io.MemoryDataStore
import lanchon.multidexlib2.BasicDexFileNamer
import lanchon.multidexlib2.DexIO
import lanchon.multidexlib2.MultiDexIO
import java.io.File
import java.io.Flushable

/**
 * A context for bytecode.
 * This holds the current state of the bytecode.
 *
 * @param options The [PatcherOptions] used to create this context.
 */
class BytecodeContext internal constructor(private val options: PatcherOptions) :
    Context<List<PatcherResult.PatchedDexFile>> {
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
                true, options.inputFile, BasicDexFileNamer(), null, null
            ).also { opcodes = it.opcodes }.classes.toMutableSet()
        )
    }

    /**
     * The [Integrations] of this [PatcherContext].
     */
    internal val integrations = Integrations(options.logger)

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
        classes.proxies.firstOrNull { predicate(it.immutableClass) } ?:
        // else resolve the class to a proxy and return it, if the predicate is matching a class
        classes.find(predicate)?.let { proxy(it) }

    /**
     * Proxy a class.
     * This will allow the class to be modified.
     *
     * @param classDef The class to proxy.
     * @return A proxy for the class.
     */
    fun proxy(classDef: ClassDef) = this.classes.proxies.find { it.immutableClass.type == classDef.type } ?: let {
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
     * The integrations of a [PatcherContext].
     *
     * @param logger The logger to use.
     */
    internal inner class Integrations(private val logger: Logger) : MutableList<File> by mutableListOf(), Flushable {
        /**
         * Whether to merge integrations.
         * True when any supplied [Patch] is annotated with [RequiresIntegrations].
         */
        var merge = false

        /**
         * Merge integrations into the [BytecodeContext] and flush all [Integrations].
         */
        override fun flush() {
            if (!merge) return

            this@Integrations.forEach { integrations ->
                MultiDexIO.readDexFile(
                    true,
                    integrations, BasicDexFileNamer(),
                    null,
                    null
                ).classes.forEach classDef@{ classDef ->
                    val existingClass = classes.find { it == classDef } ?: run {
                        logger.trace("Merging $classDef")
                        classes.add(classDef)
                        return@classDef
                    }

                    logger.trace("$classDef exists. Adding missing methods and fields.")

                    existingClass.merge(classDef, this@BytecodeContext, logger).let { mergedClass ->
                        // If the class was merged, replace the original class with the merged class.
                        if (mergedClass === existingClass) return@let
                        classes.apply { remove(existingClass); add(mergedClass) }
                    }
                }
            }

            clear()
        }
    }

    /**
     * Compile bytecode from the [BytecodeContext].
     *
     * @return The compiled bytecode.
     */
    override fun get(): List<PatcherResult.PatchedDexFile> {
        options.logger.info("Compiling modified dex files")

        return mutableMapOf<String, MemoryDataStore>().apply {
            MultiDexIO.writeDexFile(
                true, -1, // Defaults to amount of available cores.
                this, BasicDexFileNamer(), object : DexFile {
                    override fun getClasses() = this@BytecodeContext.classes.also(ProxyClassList::replaceClasses)
                    override fun getOpcodes() = this@BytecodeContext.opcodes
                }, DexIO.DEFAULT_MAX_DEX_POOL_SIZE, null
            )
        }.map { PatcherResult.PatchedDexFile(it.key, it.value.readAt(0)) }
    }
}