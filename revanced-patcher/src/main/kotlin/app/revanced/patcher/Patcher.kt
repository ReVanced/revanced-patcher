package app.revanced.patcher

import app.revanced.patcher.PatchBundleLoader.Utils.getInstance
import app.revanced.patcher.data.ResourceContext
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

    // TODO: Fix circular dependency detection.
    // /**
    //  * Add [Patch]es to ReVanced [Patcher].
    //  * It is not guaranteed that all supplied [Patch]es will be accepted, if an exception is thrown.
    //  *
    //  * @param patches The [Patch]es to add.
    //  * @throws PatcherException.CircularDependencyException If a circular dependency is detected.
    // */
    /**
     * Add [Patch]es to ReVanced [Patcher].
     *
     * @param patches The [Patch]es to add.
    */
    @Suppress("NAME_SHADOWING")
    override fun acceptPatches(patches: List<Patch<*>>) {
        /**
         * Add dependencies of a [Patch] recursively to [PatcherContext.allPatches].
         * If a [Patch] is already in [PatcherContext.allPatches], it will not be added again.
         */
        fun PatchClass.putDependenciesRecursively() {
            if (context.allPatches.contains(this)) return

            val dependency = this.java.getInstance(logger)!!
            context.allPatches[this] = dependency

            dependency.dependencies?.forEach { it.putDependenciesRecursively() }
        }

        // Add all patches and their dependencies to the context.
        for (patch in patches) context.executablePatches.putIfAbsent(patch::class, patch) ?: {
            context.allPatches[patch::class] = patch

            patch.dependencies?.forEach { it.putDependenciesRecursively() }
        }

        /* TODO: Fix circular dependency detection.
        val graph = mutableMapOf<PatchClass, MutableList<PatchClass>>()
        fun PatchClass.visit() {
            if (this in graph) return

            val group = graph.getOrPut(this) { mutableListOf(this) }

            val dependencies = context.allPatches[this]!!.manifest.dependencies ?: return
            dependencies.forEach { dependency ->
                if (group == graph[dependency])
                    throw PatcherException.CircularDependencyException(context.allPatches[this]!!.manifest.name)

                graph[dependency] = group.apply { add(dependency) }
                dependency.visit()
            }
        }
        */

        /**
         * Returns true if at least one patch or its dependencies matches the given predicate.
         *
         * @param predicate The predicate to match.
         */
        fun Patch<*>.anyRecursively(predicate: (Patch<*>) -> Boolean): Boolean =
            predicate(this) || dependencies?.any { dependency ->
                context.allPatches[dependency]!!.anyRecursively(predicate)
            } ?: false

        context.allPatches.values.let { patches ->
            // Determine, if resource patching is required.
            for (patch in patches)
                if (patch.anyRecursively { patch is ResourcePatch }) {
                    options.resourceDecodingMode = ResourceContext.ResourceDecodingMode.FULL
                    break
                }

            // Determine, if merging integrations is required.
            for (patch in patches)
                if (!patch.anyRecursively { it.requiresIntegrations }) {
                    context.bytecodeContext.integrations.merge = true
                    break
                }
        }
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

        /**
         * Execute a [Patch] and its dependencies recursively.
         *
         * @param patch The [Patch] to execute.
         * @param executedPatches A map to prevent [Patch]es from being executed twice due to dependencies.
         * @return The result of executing the [Patch].
         */
        fun executePatch(
            patch: Patch<*>,
            executedPatches: LinkedHashMap<Patch<*>, PatchResult>
        ): PatchResult {
            val patchName = patch.name

            executedPatches[patch]?.let { patchResult ->
                patchResult.exception ?: return patchResult

                // Return a new result with an exception indicating that the patch was not executed previously,
                // because it is a dependency of another patch that failed.
                return PatchResult(patch, PatchException("'$patchName' did not succeed previously"))
            }

            // Recursively execute all dependency patches.
            patch.dependencies?.forEach { dependencyName ->
                val dependency = context.executablePatches[dependencyName]!!
                val result = executePatch(dependency, executedPatches)

                result.exception?.let {
                    return PatchResult(
                        patch,
                        PatchException("'$patchName' depends on '${dependency}' that raised an exception: $it")
                    )
                }
            }

            return try {
                // TODO: Implement this in a more polymorphic way.
                when (patch) {
                    is BytecodePatch -> {
                        patch.fingerprints.toList().resolveUsingLookupMap(context.bytecodeContext)
                        patch.execute(context.bytecodeContext)
                    }
                    is ResourcePatch -> {
                        patch.execute(context.resourceContext)
                    }
                }

                PatchResult(patch)
            } catch (exception: PatchException) {
                PatchResult(patch, exception)
            } catch (exception: Exception) {
                PatchResult(patch, PatchException(exception))
            }.also { executedPatches[patch] = it }
        }

        if (context.bytecodeContext.integrations.merge) context.bytecodeContext.integrations.flush()

        MethodFingerprint.initializeFingerprintResolutionLookupMaps(context.bytecodeContext)

        // Prevent from decoding the app manifest twice if it is not needed.
        if (options.resourceDecodingMode == ResourceContext.ResourceDecodingMode.FULL)
            context.resourceContext.decodeResources(ResourceContext.ResourceDecodingMode.FULL)

        logger.info("Executing patches")

        val executedPatches = LinkedHashMap<Patch<*>, PatchResult>() // Key is name.

        context.executablePatches.values.sortedBy { it.name }.forEach { patch ->
            val patchResult = executePatch(patch, executedPatches)

            // If the patch failed, emit the result, even if it is closeable.
            // Results of executed patches that are closeable will be emitted later.
            patchResult.exception?.let {
                // Propagate exception to caller instead of wrapping it in a new exception.
                emit(patchResult)

                if (returnOnError) return@flow
            } ?: run {
                if (patch is Closeable) return@run

                emit(patchResult)
            }
        }

        executedPatches.values
            .filter { it.exception == null }
            .filter { it.patch is Closeable }.asReversed().forEach { executedPatch ->
                val patch = executedPatch.patch

                val result = try {
                    (patch as Closeable).close()

                    executedPatch
                } catch (exception: PatchException) {
                    PatchResult(patch, exception)
                } catch (exception: Exception) {
                    PatchResult(patch, PatchException(exception))
                }

                result.exception?.let {
                    emit(
                        PatchResult(
                            patch,
                            PatchException(
                                "'${patch.name}' raised an exception while being closed: $it",
                                result.exception
                            )
                        )
                    )

                    if (returnOnError) return@flow
                } ?: run {
                    patch.name ?: return@run

                    emit(result)
                }
            }
    }

    override fun close() = MethodFingerprint.clearFingerprintResolutionLookupMaps()

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

