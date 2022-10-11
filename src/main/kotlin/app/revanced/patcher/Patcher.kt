package app.revanced.patcher

import app.revanced.patcher.apk.Apk
import app.revanced.patcher.extensions.PatchExtensions.dependencies
import app.revanced.patcher.extensions.PatchExtensions.deprecated
import app.revanced.patcher.extensions.PatchExtensions.patchName
import app.revanced.patcher.fingerprint.method.impl.MethodFingerprint.Companion.resolve
import app.revanced.patcher.patch.*
import app.revanced.patcher.util.VersionReader
import brut.directory.ExtFile
import lanchon.multidexlib2.BasicDexFileNamer
import lanchon.multidexlib2.DexIO
import lanchon.multidexlib2.MultiDexIO
import org.jf.dexlib2.Opcodes
import org.jf.dexlib2.iface.DexFile
import org.jf.dexlib2.writer.io.MemoryDataStore
import java.io.File

/**
 * The ReVanced Patcher.
 * @param options The options for the patcher.
 */
class Patcher(private val options: PatcherOptions) {
    val context: PatcherContext

    private val logger = options.logger
    private val opcodes: Opcodes

    private var resourceDecodingMode = Apk.ResourceDecodingMode.MANIFEST_ONLY

    companion object {
        @Suppress("SpellCheckingInspection")
        private val dexFileNamer = BasicDexFileNamer()

        /**
         * The version of the ReVanced Patcher.
         */
        @JvmStatic
        val version = VersionReader.read()
    }

    init {
        logger.info("Reading dex files")

        // read dex files from the base apk
        MultiDexIO.readDexFile(true, options.inputFiles.base.file, dexFileNamer, null, null).also {
            opcodes = it.opcodes
        }.let {
            context = PatcherContext(it.classes.toMutableList(), File(options.resourceCacheDirectory))
        }

        // decode manifest file
        logger.info("Decoding manifest file of the base apk file")
        options.inputFiles.base.decodeResources(options, Apk.ResourceDecodingMode.MANIFEST_ONLY)
    }

    /**
     * Add [Patch]es to the patcher.
     * @param patches [Patch]es The patches to add.
     */
    fun addPatches(patches: Iterable<Class<out Patch<Context>>>) {
        /**
         * Fill the cache with the instances of the [Patch]es for later use.
         * Note: Dependencies of the [Patch] will be cached as well.
         */
        fun Class<out Patch<Context>>.isResource() {
            this.also {
                if (!ResourcePatch::class.java.isAssignableFrom(it)) return@also
                // set the mode to decode all resources before running the patches
                resourceDecodingMode = Apk.ResourceDecodingMode.FULL
            }.dependencies?.forEach { it.java.isResource() }
        }

        context.patches.addAll(patches.onEach(Class<out Patch<Context>>::isResource))
    }

    /**
     * Add additional dex file container to the patcher.
     * @param files The dex file containers to add to the patcher.
     * @param allowedOverwrites A list of class types that are allowed to be overwritten.
     */
    fun addFiles(
        files: List<File>,
        allowedOverwrites: Iterable<String> = emptyList(),
        callback: (File) -> Unit
    ) {
        for (file in files) {
            var modified = false
            for (classDef in MultiDexIO.readDexFile(true, file, dexFileNamer, null, null).classes) {
                val type = classDef.type

                val classes = context.bytecodeContext.classes.classes

                val index =  classes.indexOfFirst { it.type == type }
                if (index == -1) {
                    logger.trace("Merging $type")
                    classes.add(classDef)
                    modified = true

                    continue
                } else if (!allowedOverwrites.contains(type))
                    continue

                logger.trace("Overwriting $type")

                classes[index] = classDef
                modified = true
            }

            if (modified) callback(file)
        }
    }

    /**
     * Execute patches added the patcher.
     *
     * @param stopOnError If true, the patches will stop on the first error.
     * @return A pair of the name of the [Patch] and its [PatchResult].
     */
    fun executePatches(stopOnError: Boolean = false) = sequence {
        /**
         * Execute a [Patch] and its dependencies recursively.
         *
         * @param patchClass The [Patch] to execute.
         * @param executedPatches A map of [Patch]es paired to a boolean indicating their success, to prevent infinite recursion.
         * @return The result of executing the [Patch].
         */
        fun executePatch(
            patchClass: Class<out Patch<Context>>, executedPatches: LinkedHashMap<String, ExecutedPatch>
        ): PatchResult {
            val patchName = patchClass.patchName

            // if the patch has already executed silently skip it
            if (executedPatches.contains(patchName)) {
                if (!executedPatches[patchName]!!.success)
                    return PatchResultError("'$patchName' did not succeed previously")

                logger.trace("Skipping '$patchName' because it has already been applied")

                return PatchResultSuccess()
            }

            // recursively execute all dependency patches
            patchClass.dependencies?.forEach { dependencyClass ->
                val dependency = dependencyClass.java

                val result = executePatch(dependency, executedPatches)
                if (result.isSuccess()) return@forEach

                val error = result.error()!!
                val errorMessage = error.cause ?: error.message
                return PatchResultError("'$patchName' depends on '${dependency.patchName}' but the following error was raised: $errorMessage")
            }

            patchClass.deprecated?.let { (reason, replacement) ->
                logger.warn("'$patchName' is deprecated, reason: $reason")
                if (replacement != null) logger.warn("Use '${replacement.java.patchName}' instead")
            }

            val isResourcePatch = ResourcePatch::class.java.isAssignableFrom(patchClass)
            val patchInstance = patchClass.getDeclaredConstructor().newInstance()

            // TODO: implement this in a more polymorphic way
            val patchContext = if (isResourcePatch) {
                context.resourceContext
            } else {
                context.bytecodeContext.also { context ->
                    (patchInstance as BytecodePatch).fingerprints?.resolve(
                        context,
                        context.classes.classes
                    )
                }
            }

            logger.trace("Executing '$patchName' of type: ${if (isResourcePatch) "resource" else "bytecode"}")

            return try {
                patchInstance.execute(patchContext).also {
                    executedPatches[patchName] = ExecutedPatch(patchInstance, it.isSuccess())
                }
            } catch (e: Exception) {
                PatchResultError(e).also {
                    executedPatches[patchName] = ExecutedPatch(patchInstance, false)
                }
            }
        }

        // prevent from decoding the manifest twice if it is not needed
        //if (resourceDecodingMode == Apk.ResourceDecodingMode.FULL) {
            logger.info("Decoding resources")
            options.inputFiles.decodeResources(options, Apk.ResourceDecodingMode.FULL)
        //}

        logger.trace("Executing all patches")

        val executedPatches = LinkedHashMap<String, ExecutedPatch>() // first is name

        try {
            context.patches.forEach { patch ->
                val patchResult = executePatch(patch, executedPatches)

                val result = if (patchResult.isSuccess()) {
                    Result.success(patchResult.success()!!)
                } else {
                    Result.failure(patchResult.error()!!)
                }

                yield(patch.patchName to result)
                if (stopOnError && patchResult.isError()) return@sequence
            }
            } finally {
                executedPatches.values.reversed().forEach { (patch, _) ->
                    patch.close()
                }
            }
        }

    /**
     * Save the patched dex file.
     *
     * @return The [PatcherResult] of the [Patcher].
     */
    fun save(): PatcherResult {
       // when (resourceDecodingMode) {
            // Apk.ResourceDecodingMode.FULL -> {
                logger.info("Compiling resources")
                options.inputFiles.compileResources(options)
            // }
       //     else -> logger.info("Not compiling resources because resource patching is not required")
       // }

        // create patched dex files
        val dexFiles = mutableMapOf<String, MemoryDataStore>().also { it ->
            logger.trace("Creating new dex file")

            val newDexFile = object : DexFile {
                override fun getClasses() = context.bytecodeContext.classes.also { classes -> classes.replaceClasses() }
                override fun getOpcodes() = this@Patcher.opcodes
            }

            // write modified dex files
            logger.info("Writing modified dex files")
            MultiDexIO.writeDexFile(
                true, -1, // core count
                it, dexFileNamer, newDexFile, DexIO.DEFAULT_MAX_DEX_POOL_SIZE, null
            )
        }.map {
            app.revanced.patcher.util.dex.DexFile(it.key, it.value.readAt(0))
        }

        // save the patch to the base apk
        options.inputFiles.base.dexFiles = dexFiles

        // collect the patched files
        val patchedFiles = options.inputFiles.splits.toMutableList<Apk>().also { it.add(options.inputFiles.base) }

        return PatcherResult(patchedFiles)
    }
}

/**
 * A result of executing a [Patch].
 *
 * @param patchInstance The instance of the [Patch] that was executed.
 * @param success The result of the [Patch].
 */
internal data class ExecutedPatch(val patchInstance: Patch<Context>, val success: Boolean)