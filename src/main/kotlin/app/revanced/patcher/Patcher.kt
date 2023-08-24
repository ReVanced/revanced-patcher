package app.revanced.patcher

import app.revanced.patcher.data.Context
import app.revanced.patcher.data.ResourceContext
import app.revanced.patcher.extensions.AnnotationExtensions.findAnnotationRecursively
import app.revanced.patcher.extensions.PatchExtensions.dependencies
import app.revanced.patcher.extensions.PatchExtensions.patchName
import app.revanced.patcher.extensions.PatchExtensions.requiresIntegrations
import app.revanced.patcher.fingerprint.method.impl.MethodFingerprint
import app.revanced.patcher.fingerprint.method.impl.MethodFingerprint.Companion.resolveUsingLookupMap
import app.revanced.patcher.patch.*
import kotlinx.coroutines.flow.flow
import java.io.Closeable
import java.io.File
import java.util.function.Supplier
import java.util.logging.Level
import java.util.logging.LogManager
import java.util.logging.Logger

/**
 * ReVanced Patcher.
 *
 * @param options The options for the patcher.
 */
class Patcher(
    private val options: PatcherOptions
) : PatchExecutorFunction, PatchesConsumer, IntegrationsConsumer, Supplier<PatcherResult>, Closeable {
    private val logger = Logger.getLogger(Patcher::class.java.name)

    /**
     * The context of ReVanced [Patcher].
     * This holds the current state of the patcher.
     */
    val context = PatcherContext(options)

    init {
        LogManager.getLogManager().let { manager ->
            // Disable root logger.
            manager.getLogger("").level = Level.OFF

            // Enable ReVanced logging only.
            manager.loggerNames
                .toList()
                .filter { it.startsWith("app.revanced") }
                .map { manager.getLogger(it) }
                .forEach { it.level = Level.INFO }
        }

        context.resourceContext.decodeResources(ResourceContext.ResourceDecodingMode.MANIFEST_ONLY)
    }

    override fun acceptPatches(patches: List<PatchClass>) {
        /**
         * Returns true if at least one patches or its dependencies matches the given predicate.
         */
        fun PatchClass.anyRecursively(predicate: (PatchClass) -> Boolean): Boolean =
            predicate(this) || dependencies?.any { dependency ->
                dependency.java.anyRecursively(predicate)
            } ?: false

        // Determine if resource patching is required.
        for (patch in patches) {
            if (patch.anyRecursively { ResourcePatch::class.java.isAssignableFrom(it) }) {
                options.resourceDecodingMode = ResourceContext.ResourceDecodingMode.FULL
                break
            }
        }

        // Determine if merging integrations is required.
        for (patch in patches) {
            if (patch.anyRecursively { it.requiresIntegrations }) {
                context.bytecodeContext.integrations.merge = true
                break
            }
        }

        context.patches.addAll(patches)
    }

    /**
     * Add integrations to the [Patcher].
     *
     * @param integrations The integrations to add. Must be a DEX file or container of DEX files.
     */
    override fun acceptIntegrations(integrations: List<File>) {
        context.bytecodeContext.integrations.addAll(integrations)
    }

    /**
     * Execute [Patch]es that were added to ReVanced [Patcher].
     *
     * @param returnOnError If true, ReVanced [Patcher] will return immediately if a [Patch] fails.
     * @return A pair of the name of the [Patch] and its [PatchResult].
     */
    override fun apply(returnOnError: Boolean) = flow {
        class ExecutedPatch(val patchInstance: Patch<Context<*>>, val patchResult: PatchResult)

        /**
         * Execute a [Patch] and its dependencies recursively.
         *
         * @param patchClass The [Patch] to execute.
         * @param executedPatches A map to prevent [Patch]es from being executed twice due to dependencies.
         * @return The result of executing the [Patch].
         */
        fun executePatch(
            patchClass: PatchClass,
            executedPatches: LinkedHashMap<String, ExecutedPatch>
        ): PatchResult {
            val patchName = patchClass.patchName

            executedPatches[patchName]?.let { executedPatch ->
                executedPatch.patchResult.exception ?: return executedPatch.patchResult

                // Return a new result with an exception indicating that the patch was not executed previously,
                // because it is a dependency of another patch that failed.
                return PatchResult(patchName, PatchException("'$patchName' did not succeed previously"))
            }

            // Recursively execute all dependency patches.
            patchClass.dependencies?.forEach { dependencyClass ->
                val dependency = dependencyClass.java

                val result = executePatch(dependency, executedPatches)

                result.exception?.let {
                    return PatchResult(
                        patchName,
                        PatchException(
                            "'$patchName' depends on '${dependency.patchName}' that raised an exception: $it"
                        )
                    )
                }
            }

            // TODO: Implement this in a more polymorphic way.
            val patchInstance = patchClass.getDeclaredConstructor().newInstance()

            val patchContext = if (patchInstance is BytecodePatch) {
                patchInstance.fingerprints?.resolveUsingLookupMap(context.bytecodeContext)

                context.bytecodeContext
            } else {
                context.resourceContext
            }

            return try {
                patchInstance.execute(patchContext)

                PatchResult(patchName)
            } catch (exception: PatchException) {
                PatchResult(patchName, exception)
            } catch (exception: Exception) {
                PatchResult(patchName, PatchException(exception))
            }.also { executedPatches[patchName] = ExecutedPatch(patchInstance, it) }
        }

        if (context.bytecodeContext.integrations.merge) context.bytecodeContext.integrations.flush()

        MethodFingerprint.initializeFingerprintResolutionLookupMaps(context.bytecodeContext)

        // Prevent from decoding the app manifest twice if it is not needed.
        if (options.resourceDecodingMode == ResourceContext.ResourceDecodingMode.FULL)
            context.resourceContext.decodeResources(ResourceContext.ResourceDecodingMode.FULL)

        logger.info("Executing patches")

        val executedPatches = LinkedHashMap<String, ExecutedPatch>() // Key is name.

        context.patches.forEach { patch ->
            val result = executePatch(patch, executedPatches)

            // If the patch failed, or if the patch is not closeable, emit the result.
            // Results of patches that are closeable will be emitted later.
            result.exception?.let {
                emit(result)

                if (returnOnError) return@flow
            } ?: run {
                if (executedPatches[result.patchName]!!.patchInstance is Closeable) return@run

                emit(result)
            }
        }

        executedPatches.values
            .filter { it.patchResult.exception == null }
            .filter { it.patchInstance is Closeable }.asReversed().forEach { executedPatch ->
                val patchName = executedPatch.patchResult.patchName

                val result = try {
                    (executedPatch.patchInstance as Closeable).close()

                    executedPatch.patchResult
                } catch (exception: PatchException) {
                    PatchResult(patchName, exception)
                } catch (exception: Exception) {
                    PatchResult(patchName, PatchException(exception))
                }

                result.exception?.let {
                    emit(
                        PatchResult(
                            patchName,
                            PatchException("'$patchName' raised an exception while being closed: $it")
                        )
                    )

                    if (returnOnError) return@flow
                } ?: run {
                    executedPatch
                        .patchInstance::class
                        .java
                        .findAnnotationRecursively(app.revanced.patcher.patch.annotations.Patch::class)
                        ?: return@run

                    emit(result)
                }
            }
    }

    override fun close() {
        MethodFingerprint.clearFingerprintResolutionLookupMaps()
    }

    /**
     * Compile and save the patched APK file.
     *
     * @return The [PatcherResult] containing the patched input files.
     */
    override fun get() = PatcherResult(
        context.bytecodeContext.get(),
        context.resourceContext.get(),
        context.packageMetadata.apkInfo.doNotCompress?.toList()
    )
}

