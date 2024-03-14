package app.revanced.patcher

import app.revanced.patcher.PatchBundleLoader.Utils.getInstance
import app.revanced.patcher.data.ResourceContext
import app.revanced.patcher.fingerprint.LookupMap
import app.revanced.patcher.patch.*
import kotlinx.coroutines.flow.flow
import java.io.Closeable
import java.io.File
import java.util.function.Supplier
import java.util.logging.Logger

/**
 * A Patcher.
 *
 * @param config The configuration to use for the patcher.
 */
class Patcher(
    private val config: PatcherConfig,
) : PatchExecutorFunction, PatchesConsumer, IntegrationsConsumer, Supplier<PatcherResult>, Closeable {
    private val logger = Logger.getLogger(Patcher::class.java.name)

    /**
     * A context for the patcher containing the current state of the patcher.
     */
    val context = PatcherContext(config)

    @Suppress("DEPRECATION")
    @Deprecated("Use Patcher(PatcherConfig) instead.")
    constructor(
        patcherOptions: PatcherOptions,
    ) : this(
        PatcherConfig(
            patcherOptions.inputFile,
            patcherOptions.resourceCachePath,
            patcherOptions.aaptBinaryPath,
            patcherOptions.frameworkFileDirectory,
            patcherOptions.multithreadingDexFileWriter,
        ),
    )

    init {
        context.resourceContext.decodeResources(ResourceContext.ResourceMode.NONE)
    }

    /**
     * Add [Patch]es to ReVanced [Patcher].
     *
     * @param patches The [Patch]es to add.
     */
    @Suppress("NAME_SHADOWING")
    override fun acceptPatches(patches: PatchSet) {
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
        patches.forEach { patch ->
            context.executablePatches.putIfAbsent(patch::class, patch) ?: run {
                context.allPatches[patch::class] = patch

                patch.dependencies?.forEach { it.putDependenciesRecursively() }
            }
        }

        // TODO: Detect circular dependencies.

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
            // Determine the resource mode.

            config.resourceMode = if (patches.any { patch -> patch.anyRecursively { it is ResourcePatch } }) {
                ResourceContext.ResourceMode.FULL
            } else if (patches.any { patch -> patch.anyRecursively { it is RawResourcePatch } }) {
                ResourceContext.ResourceMode.RAW_ONLY
            } else {
                ResourceContext.ResourceMode.NONE
            }

            // Determine, if merging integrations is required.
            for (patch in patches)
                if (patch.anyRecursively { it.requiresIntegrations }) {
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
    override fun acceptIntegrations(integrations: Set<File>) {
        context.bytecodeContext.integrations.addAll(integrations)
    }

    @Deprecated(
        "Use acceptIntegrations(Set<File>) instead.",
        ReplaceWith("acceptIntegrations(integrations.toSet())"),
    )
    override fun acceptIntegrations(integrations: List<File>) = acceptIntegrations(integrations.toSet())

    /**
     * Execute [Patch]es that were added to ReVanced [Patcher].
     *
     * @param returnOnError If true, ReVanced [Patcher] will return immediately if a [Patch] fails.
     * @return A pair of the name of the [Patch] and its [PatchResult].
     */
    override fun apply(returnOnError: Boolean) =
        flow {
            /**
             * Execute a [Patch] and its dependencies recursively.
             *
             * @param patch The [Patch] to execute.
             * @param executedPatches A map to prevent [Patch]es from being executed twice due to dependencies.
             * @return The result of executing the [Patch].
             */
            fun executePatch(
                patch: Patch<*>,
                executedPatches: LinkedHashMap<Patch<*>, PatchResult>,
            ): PatchResult {
                val patchName = patch.toString()

                executedPatches[patch]?.let { patchResult ->
                    patchResult.exception ?: return patchResult

                    // Return a new result with an exception indicating that the patch was not executed previously,
                    // because it is a dependency of another patch that failed.
                    return PatchResult(patch, PatchException("'$patchName' did not succeed previously"))
                }

                // Recursively execute all dependency patches.
                patch.dependencies?.forEach { dependencyClass ->
                    val dependency = context.allPatches[dependencyClass]!!
                    val result = executePatch(dependency, executedPatches)

                    result.exception?.let {
                        return PatchResult(
                            patch,
                            PatchException(
                                "'$patchName' depends on '${dependency.name ?: dependency}' " +
                                    "that raised an exception:\n${it.stackTraceToString()}",
                            ),
                        )
                    }
                }

                return try {
                    patch.execute(context)

                    PatchResult(patch)
                } catch (exception: PatchException) {
                    PatchResult(patch, exception)
                } catch (exception: Exception) {
                    PatchResult(patch, PatchException(exception))
                }.also { executedPatches[patch] = it }
            }

            if (context.bytecodeContext.integrations.merge) context.bytecodeContext.integrations.flush()

            LookupMap.initializeLookupMaps(context.bytecodeContext)

            // Prevent from decoding the app manifest twice if it is not needed.
            if (config.resourceMode != ResourceContext.ResourceMode.NONE) {
                context.resourceContext.decodeResources(config.resourceMode)
            }

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

                    val result =
                        try {
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
                                    "'$patch' raised an exception while being closed: ${it.stackTraceToString()}",
                                    result.exception,
                                ),
                            ),
                        )

                        if (returnOnError) return@flow
                    } ?: run {
                        patch.name ?: return@run

                        emit(result)
                    }
                }
        }

    override fun close() = LookupMap.clearLookupMaps()

    /**
     * Compile and save the patched APK file.
     *
     * @return The [PatcherResult] containing the patched input files.
     */
    @OptIn(InternalApi::class)
    override fun get() =
        PatcherResult(
            context.bytecodeContext.get(),
            context.resourceContext.get(),
        )
}
