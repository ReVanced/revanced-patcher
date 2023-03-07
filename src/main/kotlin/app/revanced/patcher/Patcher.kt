package app.revanced.patcher

import app.revanced.patcher.apk.Apk
import app.revanced.patcher.extensions.PatchExtensions.dependencies
import app.revanced.patcher.extensions.PatchExtensions.patchName
import app.revanced.patcher.extensions.PatchExtensions.requiresIntegrations
import app.revanced.patcher.fingerprint.method.impl.MethodFingerprint.Companion.resolve
import app.revanced.patcher.patch.*
import app.revanced.patcher.util.VersionReader
import lanchon.multidexlib2.BasicDexFileNamer
import java.io.File

/**
 * The ReVanced Patcher.
 * @param options The options for the patcher.
 */
class Patcher(private val options: PatcherOptions) {
    private val context = PatcherContext(options)
    private val logger = options.logger
    private var mergeIntegrations = false
    private var decodingMode = Apk.ResourceDecodingMode.MANIFEST_ONLY

    companion object {
        @Suppress("SpellCheckingInspection")
        internal val dexFileNamer = BasicDexFileNamer()

        /**
         * The version of the ReVanced Patcher.
         */
        @JvmStatic
        val version = VersionReader.read()
    }

    init {
        // Decode manifest file.
        logger.info("Decoding manifest file of the base apk file")

        options.apkBundle.base.decodeResources(options, Apk.ResourceDecodingMode.MANIFEST_ONLY)
    }

    /**
     * Add integrations to be merged by the patcher.
     * The integrations will only be merged, if necessary.
     *
     * @param integrations The integrations, must be dex files or dex file container such as ZIP, APK or DEX files.
     */
    fun addIntegrations(integrations: List<File>) = context.integrations.add(integrations)

    /**
     * Add [Patch]es to the patcher.
     * @param patches [Patch]es The patches to add.
     */
    /**
     * Add [Patch]es to the patcher.
     * @param patches [Patch]es The patches to add.
     */
    fun addPatches(patches: Iterable<PatchClass>) {
        /**
         * Returns true if at least one patches or its dependencies matches the given predicate.
         */
        fun PatchClass.anyRecursively(predicate: (PatchClass) -> Boolean): Boolean =
            predicate(this) || dependencies?.any { it.java.anyRecursively(predicate) } == true


        // Determine if resource patching is required.
        for (patch in patches) {
            if (patch.anyRecursively { ResourcePatch::class.java.isAssignableFrom(it) }) {
                decodingMode = Apk.ResourceDecodingMode.FULL
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
     * Execute the patcher.
     *
     * @param stopOnError If true, the patches will stop on the first error.
     * @return A pair of the name of the [Patch] and its [PatchResult].
     */
    fun execute(stopOnError: Boolean = false) = sequence {
        /**
         * Execute a [Patch] and its dependencies recursively.
         *
         * @param patchClass The [Patch] to execute.
         * @param executedPatches A map of [Patch]es paired to a boolean indicating their success, to prevent infinite recursion.
         * @return The result of executing the [Patch].
         */
        fun executePatch(
            patchClass: PatchClass,
            executedPatches: HashMap<String, ExecutedPatch>
        ): PatchResult {
            val patchName = patchClass.patchName

            // If the patch has already executed silently skip it.
            if (executedPatches.contains(patchName)) {
                if (!executedPatches[patchName]!!.success)
                    return PatchResult.Error("'$patchName' did not succeed previously")

                logger.trace("Skipping '$patchName' because it has already been executed")

                return PatchResult.Success
            }

            // Recursively execute all dependency patches.
            patchClass.dependencies?.forEach { dependencyClass ->
                val dependency = dependencyClass.java

                executePatch(dependency, executedPatches).also {
                    if (it is PatchResult.Success) return@forEach
                }.let {
                    with(it as PatchResult.Error) {
                        val errorMessage = it.cause?.stackTraceToString() ?: it.message
                        return PatchResult.Error(
                            "'$patchName' depends on '${dependency.patchName}' " +
                                    "but the following exception was raised: $errorMessage",
                            it
                        )
                    }
                }
            }

            val isResourcePatch = ResourcePatch::class.java.isAssignableFrom(patchClass)
            val patchInstance = patchClass.getDeclaredConstructor().newInstance()

            // TODO: implement this in a more polymorphic way.
            val patchContext = if (isResourcePatch) {
                context.resourceContext
            } else {
                context.bytecodeContext.apply {
                    val bytecodePatch = patchInstance as BytecodePatch
                    bytecodePatch.fingerprints?.resolve(this, classes)
                }
            }

            logger.trace("Executing '$patchName' of type: ${if (isResourcePatch) "resource" else "bytecode"}")

            return try {
                patchInstance.execute(patchContext)
            } catch (patchException: PatchResult.Error) {
                patchException
            } catch (exception: Exception) {
                PatchResult.Error("Unhandled patch exception: ${exception.message}", exception)
            }.also {
                executedPatches[patchName] = ExecutedPatch(patchInstance, it is PatchResult.Success)
            }
        }
        if (mergeIntegrations) context.integrations.merge(logger, dexFileNamer)

        // Prevent from decoding the manifest twice if it is not needed.
        if (decodingMode == Apk.ResourceDecodingMode.FULL) {
            options.apkBundle.decodeResources(options, Apk.ResourceDecodingMode.FULL).forEach {
                logger.info("Decoding resources for $it apk file")
            }

            // region Workaround because Androlib does not support split apk files

            options.apkBundle.also {
                logger.info("Merging split apk resources to base apk resources")
            }.mergeResources(options)

            // endregion
        }

        logger.trace("Executing all patches")

        HashMap<String, ExecutedPatch>().apply {
            try {
                context.patches.forEach { patch ->
                    val result = executePatch(patch, this)

                    yield(patch.patchName to result)
                    if (stopOnError && result is PatchResult.Error) return@sequence
                }
            } finally {
                values.reversed().forEach { (patch, _) ->
                    patch.close()
                }
            }
        }
    }

    /**
     * Save the patched dex file.
     *
     * @return The [PatcherResult] of the [Patcher].
     */
    fun save(): PatcherResult {
        val patchResults = buildList {
            if (decodingMode == Apk.ResourceDecodingMode.FULL) {
                logger.info("Writing patched resources")
                options.apkBundle.writeResources(options).forEach { writeResult ->
                    if (writeResult.exception is Apk.ApkException.Write) return@forEach

                    val patch = writeResult.apk.let {
                        when (it) {
                            is Apk.Base -> PatcherResult.Patch.Base(it)
                            is Apk.Split -> PatcherResult.Patch.Split(it)
                        }
                    }

                    add(patch)

                    logger.info("Patched resources written for ${writeResult.apk} apk file")
                }
            }
        }

        options.apkBundle.base.apply {
            logger.info("Writing patched dex files")
            dexFiles = bytecodeData.writeDexFiles()
        }

        return PatcherResult(patchResults)
    }
}

/**
 * A result of executing a [Patch].
 *
 * @param patchInstance The instance of the [Patch] that was executed.
 * @param success The result of the [Patch].
 */
internal data class ExecutedPatch(val patchInstance: Patch<Context>, val success: Boolean)