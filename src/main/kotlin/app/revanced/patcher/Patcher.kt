package app.revanced.patcher

import app.revanced.patcher.patch.*
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.channelFlow
import java.io.Closeable
import java.util.concurrent.ConcurrentHashMap
import java.util.logging.Logger

/**
 * A Patcher.
 *
 * @param config The configuration to use for the patcher.
 */
class Patcher(private val config: PatcherConfig) : Closeable {
    private val logger = Logger.getLogger(this::class.java.name)

    /**
     * The context containing the current state of the patcher.
     */
    val context = PatcherContext(config)

    init {
        context.resourceContext.decodeResources(ResourcePatchContext.ResourceMode.NONE)
    }

    /**
     * Add patches.
     *
     * @param patches The patches to add.
     */
    operator fun plusAssign(patches: Set<Patch<*>>) {
        // Add all patches to the executablePatches set.
        context.executablePatches += patches

        // Add all patches and their dependencies to the allPatches set.
        patches.forEach { patch ->
            fun Patch<*>.addRecursively() =
                also(context.allPatches::add).dependencies.forEach(Patch<*>::addRecursively)

            patch.addRecursively()
        }

        context.allPatches.let { allPatches ->
            // Check, if what kind of resource mode is required.
            config.resourceMode = if (allPatches.any { patch -> patch.anyRecursively { it is ResourcePatch } }) {
                ResourcePatchContext.ResourceMode.FULL
            } else if (allPatches.any { patch -> patch.anyRecursively { it is RawResourcePatch } }) {
                ResourcePatchContext.ResourceMode.RAW_ONLY
            } else {
                ResourcePatchContext.ResourceMode.NONE
            }
        }
    }

    /**
     * Execute added patches.
     *
     * @return A flow of [PatchResult]s.
     */
    operator fun invoke() = channelFlow {
        // Prevent decoding the app manifest twice if it is not needed.
        if (config.resourceMode != ResourcePatchContext.ResourceMode.NONE) {
            context.resourceContext.decodeResources(config.resourceMode)
        }

        logger.info("Initializing lookup maps")

        // Accessing the lazy lookup maps to initialize them.
        context.bytecodeContext.lookupMaps

        logger.info("Executing patches")

        val executedPatches = ConcurrentHashMap<Patch<*>, Deferred<PatchResult>>()

        suspend operator fun Patch<*>.invoke(): Deferred<PatchResult> {
            val patch = this

            // If the patch was executed before or failed, return it's the result.
            executedPatches[patch]?.let { deferredPatchResult ->
                val patchResult = deferredPatchResult.await()

                patchResult.exception ?: return deferredPatchResult

                return CompletableDeferred(PatchResult(patch, PatchException("The patch '$patch' failed previously")))
            }

            return async(Dispatchers.IO) {
                // Recursively execute all dependency patches.
                val dependenciesResult = coroutineScope {
                    val dependenciesJobs = dependencies.map { dependency ->
                        async(Dispatchers.IO) {
                            dependency().await().exception?.let { exception ->
                                PatchResult(
                                    patch,
                                    PatchException(
                                        "The patch \"$patch\" depends on \"$dependency\", which raised an exception:\n" +
                                            exception.stackTraceToString(),
                                    ),
                                )
                            }
                        }
                    }

                    dependenciesJobs.awaitAll().firstOrNull { result -> result != null }?.let {
                        dependenciesJobs.forEach(Deferred<*>::cancel)

                        return@coroutineScope it
                    }
                }

                if (dependenciesResult != null) {
                    return@async dependenciesResult
                }

                // Execute the patch.
                try {
                    execute(context)

                    PatchResult(patch)
                } catch (exception: PatchException) {
                    PatchResult(patch, exception)
                } catch (exception: Exception) {
                    PatchResult(patch, PatchException(exception))
                }
            }.also { executedPatches[patch] = it }
        }

        coroutineScope {
            context.executablePatches.sortedBy { it.name }.map { patch ->
                launch(Dispatchers.IO) {
                    val patchResult = patch().await()

                    // If an exception occurred or the patch has no finalize block, emit the result.
                    if (patchResult.exception != null || patch.finalizeBlock == null) {
                        send(patchResult)
                    }
                }
            }.joinAll()
        }

        val succeededPatchesWithFinalizeBlock = executedPatches.values.map { it.await() }.filter {
            it.exception == null && it.patch.finalizeBlock != null
        }

        coroutineScope {
            succeededPatchesWithFinalizeBlock.asReversed().map { executionResult ->
                launch(Dispatchers.IO) {
                    val patch = executionResult.patch

                    val result =
                        try {
                            patch.finalize(context)

                            executionResult
                        } catch (exception: PatchException) {
                            PatchResult(patch, exception)
                        } catch (exception: Exception) {
                            PatchResult(patch, PatchException(exception))
                        }

                    if (result.exception != null) {
                        send(
                            PatchResult(
                                patch,
                                PatchException(
                                    "The patch \"$patch\" raised an exception during finalization:\n" +
                                        result.exception.stackTraceToString(),
                                    result.exception,
                                ),
                            ),
                        )
                    } else if (patch in context.executablePatches) {
                        send(result)
                    }
                }
            }.joinAll()
        }
    }

    override fun close() = context.close()

    /**
     * Compile and save patched APK files.
     *
     * @return The [PatcherResult] containing the patched APK files.
     */
    @OptIn(InternalApi::class)
    fun get() = PatcherResult(context.bytecodeContext.get(), context.resourceContext.get())
}
