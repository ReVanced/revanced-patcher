package app.revanced.patcher

import app.revanced.patcher.data.Context
import app.revanced.patcher.extensions.PatchExtensions.dependencies
import app.revanced.patcher.extensions.PatchExtensions.patchName
import app.revanced.patcher.extensions.PatchExtensions.requiresIntegrations
import app.revanced.patcher.fingerprint.method.impl.MethodFingerprint
import app.revanced.patcher.fingerprint.method.impl.MethodFingerprint.Companion.resolveUsingLookupMap
import app.revanced.patcher.patch.*
import brut.androlib.AaptInvoker
import brut.androlib.ApkDecoder
import brut.androlib.Config
import brut.androlib.apk.ApkInfo
import brut.androlib.apk.UsesFramework
import brut.androlib.res.Framework
import brut.androlib.res.ResourcesDecoder
import brut.androlib.res.decoder.AndroidManifestResourceParser
import brut.androlib.res.decoder.XmlPullStreamDecoder
import brut.androlib.res.xml.ResXmlPatcher
import brut.directory.ExtFile
import com.android.tools.smali.dexlib2.Opcodes
import com.android.tools.smali.dexlib2.iface.DexFile
import com.android.tools.smali.dexlib2.writer.io.MemoryDataStore
import lanchon.multidexlib2.BasicDexFileNamer
import lanchon.multidexlib2.DexIO
import lanchon.multidexlib2.MultiDexIO
import java.io.Closeable
import java.io.File
import java.io.OutputStream
import java.nio.file.Files
import java.util.logging.Level
import java.util.logging.LogManager

internal val NAMER = BasicDexFileNamer()

/**
 * The ReVanced Patcher.
 * @param options The options for the patcher.
 */
class Patcher(private val options: PatcherOptions) {
    val context: PatcherContext

    private val logger = options.logger

    private val opcodes: Opcodes

    private var resourceDecodingMode = ResourceDecodingMode.MANIFEST_ONLY

    private var mergeIntegrations = false

    private val config = Config.getDefaultConfig().apply {
        useAapt2 = true
        aaptPath = options.aaptPath
        frameworkDirectory = options.frameworkDirectory
    }

    init {
        // Disable unwanted logging.
        LogManager.getLogManager().let { manager ->
            manager.getLogger("").level = Level.OFF // Disable root logger.
            // Enable only ReVanced logging.
            manager.loggerNames
                .toList()
                .filter { it.startsWith("app.revanced") }
                .map { manager.getLogger(it) }
                .forEach { it.level = Level.INFO }
        }

        logger.info("Reading dex files")

        // read dex files
        val dexFile = MultiDexIO.readDexFile(true, options.inputFile, NAMER, null, null)

        // get the opcodes
        opcodes = dexFile.opcodes

        // finally create patcher context
        context = PatcherContext(dexFile.classes.toMutableList(), File(options.resourceCacheDirectory))

        // decode manifest file
        decodeResources(ResourceDecodingMode.MANIFEST_ONLY)
    }

    /**
     * Add integrations to be merged by the patcher.
     * The integrations will only be merged, if necessary.
     *
     * @param integrations The integrations, must be dex files or dex file container such as ZIP, APK or DEX files.
     * @param callback The callback for [integrations] which are being added.
     */
    fun addIntegrations(
        integrations: List<File>,
        callback: (File) -> Unit
    ) {
        context.integrations.apply integrations@{
            add(integrations)
            this@integrations.callback = callback
        }
    }

    /**
     * Save the patched dex file.
     */
    fun save(): PatcherResult {
        var resourceFile: File? = null

        if (resourceDecodingMode == ResourceDecodingMode.FULL) {
            logger.info("Compiling resources")

            val cacheDirectory = ExtFile(options.resourceCacheDirectory)
            val aaptFile = cacheDirectory.resolve("aapt_temp_file").also {
                Files.deleteIfExists(it.toPath())
            }.also { resourceFile = it }

            try {
                AaptInvoker(
                    config,
                    context.packageMetadata.apkInfo
                ).invokeAapt(
                    aaptFile,
                    cacheDirectory.resolve("AndroidManifest.xml").also {
                        ResXmlPatcher.fixingPublicAttrsInProviderAttributes(it)
                    },
                    cacheDirectory.resolve("res"),
                    null,
                    null,
                    context.packageMetadata.apkInfo.usesFramework.let { usesFramework ->
                        usesFramework.ids.map { id ->
                            Framework(config).getFrameworkApk(id, usesFramework.tag)
                        }.toTypedArray()
                    }
                )
            } finally {
                cacheDirectory.close()
            }
        }

        logger.info("Writing modified dex files")

        return mutableMapOf<String, MemoryDataStore>().apply {
            MultiDexIO.writeDexFile(
                true,
                -1, // Defaults to amount of available cores.
                this,
                NAMER,
                object : DexFile {
                    override fun getClasses() = context.bytecodeContext.classes.also { it.replaceClasses() }
                    override fun getOpcodes() = this@Patcher.opcodes
                },
                DexIO.DEFAULT_MAX_DEX_POOL_SIZE,
                null
            )
        }.let { dexFiles ->
            PatcherResult(
                dexFiles.map {
                    app.revanced.patcher.util.dex.DexFile(it.key, it.value.readAt(0))
                },
                context.packageMetadata.apkInfo.doNotCompress?.toList(),
                resourceFile
            )
        }
    }

    /**
     * Add [Patch]es to the patcher.
     * @param patches [Patch]es The patches to add.
     */
    fun addPatches(patches: Iterable<Class<out Patch<Context>>>) {
        /**
         * Returns true if at least one patches or its dependencies matches the given predicate.
         */
        fun Class<out Patch<Context>>.anyRecursively(predicate: (Class<out Patch<Context>>) -> Boolean): Boolean =
            predicate(this) || dependencies?.any { it.java.anyRecursively(predicate) } == true


        // Determine if resource patching is required.
        for (patch in patches) {
            if (patch.anyRecursively { ResourcePatch::class.java.isAssignableFrom(it) }) {
                resourceDecodingMode = ResourceDecodingMode.FULL
                break
            }
        }

        // Determine if merging integrations is required.
        for (patch in patches) {
            if (patch.anyRecursively { it.requiresIntegrations }) {
                mergeIntegrations = true
                break
            }
        }

        context.patches.addAll(patches)
    }

    /**
     * Decode resources for the patcher.
     *
     * @param mode The [ResourceDecodingMode] to use when decoding.
     */
    private fun decodeResources(mode: ResourceDecodingMode) {
        val apkInfo = ApkInfo(ExtFile(options.inputFile)).also { context.packageMetadata.apkInfo = it }

        // Needed to record uncompressed files.
        val apkDecoder = ApkDecoder(config, apkInfo)

        // Needed to decode resources.
        val resourcesDecoder = ResourcesDecoder(config, apkInfo)

        try {
            when (mode) {
                ResourceDecodingMode.FULL -> {
                    logger.info("Decoding resources")

                    val outDir = options.recreateResourceCacheDirectory()

                    resourcesDecoder.decodeResources(outDir)
                    resourcesDecoder.decodeManifest(outDir)

                    apkDecoder.recordUncompressedFiles(resourcesDecoder.resFileMapping)

                    apkInfo.usesFramework = UsesFramework().apply {
                        ids = resourcesDecoder.resTable.listFramePackages().map { it.id }
                    }
                }
                ResourceDecodingMode.MANIFEST_ONLY -> {
                    logger.info("Decoding app manifest")

                    // Decode manually instead of using resourceDecoder.decodeManifest
                    // because it does not support decoding to an OutputStream.
                    XmlPullStreamDecoder(
                        AndroidManifestResourceParser(resourcesDecoder.resTable),
                        resourcesDecoder.resXmlSerializer
                    ).decodeManifest(
                        apkInfo.apkFile.directory.getFileInput("AndroidManifest.xml"),
                        // Older Android versions do not support OutputStream.nullOutputStream()
                        object : OutputStream() {
                            override fun write(b: Int) { /* do nothing */
                            }
                        }
                    )

                    // Get the package name and version from the manifest using the XmlPullStreamDecoder.
                    // XmlPullStreamDecoder.decodeManifest() sets metadata.apkInfo.
                    context.packageMetadata.let { metadata ->
                        metadata.packageName = resourcesDecoder.resTable.packageRenamed
                        apkInfo.versionInfo.let {
                            metadata.packageVersion = it.versionName ?: it.versionCode
                        }
                    }
                }
            }
        } finally {
            apkInfo.apkFile.close()
        }
    }

    /**
     * Execute patches added the patcher.
     *
     * @param stopOnError If true, the patches will stop on the first error.
     * @return A pair of the name of the [Patch] and its [PatchResult].
     */
    fun executePatches(stopOnError: Boolean = false): Sequence<Pair<String, Result<PatchResultSuccess>>> {
        /**
         * Execute a [Patch] and its dependencies recursively.
         *
         * @param patchClass The [Patch] to execute.
         * @param executedPatches A map of [Patch]es paired to a boolean indicating their success, to prevent infinite recursion.
         * @return The result of executing the [Patch].
         */
        fun executePatch(
            patchClass: Class<out Patch<Context>>,
            executedPatches: LinkedHashMap<String, ExecutedPatch>
        ): PatchResult {
            val patchName = patchClass.patchName

            // if the patch has already applied silently skip it
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

                return PatchResultError(
                    "'$patchName' depends on '${dependency.patchName}' but the following error was raised: " +
                            result.error()!!.let { it.cause?.stackTraceToString() ?: it.message }
                )
            }

            val isResourcePatch = ResourcePatch::class.java.isAssignableFrom(patchClass)
            val patchInstance = patchClass.getDeclaredConstructor().newInstance()

            // TODO: implement this in a more polymorphic way
            val patchContext = if (isResourcePatch) {
                context.resourceContext
            } else {
                context.bytecodeContext.also { context ->
                    (patchInstance as BytecodePatch).fingerprints?.resolveUsingLookupMap(context)
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

        return sequence {
            if (mergeIntegrations) context.integrations.merge(logger)

            logger.trace("Initialize lookup maps for method MethodFingerprint resolution")

            MethodFingerprint.initializeFingerprintResolutionLookupMaps(context.bytecodeContext)

            // prevent from decoding the manifest twice if it is not needed
            if (resourceDecodingMode == ResourceDecodingMode.FULL) decodeResources(ResourceDecodingMode.FULL)

            logger.info("Executing patches")

            val executedPatches = LinkedHashMap<String, ExecutedPatch>() // first is name

            context.patches.forEach { patch ->
                val patchResult = executePatch(patch, executedPatches)

                val result = if (patchResult.isSuccess()) {
                    Result.success(patchResult.success()!!)
                } else {
                    Result.failure(patchResult.error()!!)
                }

                // TODO: This prints before the patch really finishes in case it is a Closeable
                //  because the Closeable is closed after all patches are executed.
                yield(patch.patchName to result)

                if (stopOnError && patchResult.isError()) return@sequence
            }

            executedPatches.values
                .filter(ExecutedPatch::success)
                .map(ExecutedPatch::patchInstance)
                .filterIsInstance(Closeable::class.java)
                .asReversed().forEach {
                    try {
                        it.close()
                    } catch (exception: Exception) {
                        val patchName = (it as Patch<Context>).javaClass.patchName

                        logger.error("Failed to close '$patchName': ${exception.stackTraceToString()}")

                        yield(patchName to Result.failure(exception))

                        // This is not failsafe. If a patch throws an exception while closing,
                        // the other patches that depend on it may fail.
                        if (stopOnError) return@sequence
                    }
                }

            MethodFingerprint.clearFingerprintResolutionLookupMaps()
        }
    }

    /**
     * The type of decoding the resources.
     */
    private enum class ResourceDecodingMode {
        /**
         * Decode all resources.
         */
        FULL,

        /**
         * Decode the manifest file only.
         */
        MANIFEST_ONLY,
    }
}

/**
 * A result of executing a [Patch].
 *
 * @param patchInstance The instance of the [Patch] that was applied.
 * @param success The result of the [Patch].
 */
internal data class ExecutedPatch(val patchInstance: Patch<Context>, val success: Boolean)
