package app.revanced.patcher

import app.revanced.patcher.patch.*
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import java.io.Closeable
import java.io.File
import java.util.function.Function
import java.util.function.Supplier
import java.util.logging.Logger

@FunctionalInterface
interface PatchesConsumer {
    fun accept(patches: Set<Patch<*>>, integrations: Set<File> = emptySet())
}

@FunctionalInterface
interface PatcherResultSupplier :
    Supplier<PatcherResult>,
    Closeable

@FunctionalInterface
interface PatchExecutorFunction : Function<Boolean, Flow<PatchResult>>

/**
 * A Patcher.
 *
 * @param config The configuration to use for the patcher.
 */
class Patcher(
    private val config: PatcherConfig,
) : PatchExecutorFunction,
    PatchesConsumer,
    PatcherResultSupplier {
    private val logger = Logger.getLogger(Patcher::class.java.name)

    /**
     * A context for the patcher containing the current state of the patcher.
     */
    val context = PatcherContext(config)

    init {
        context.resourceContext.decodeResources(ResourcePatchContext.ResourceMode.NONE)
    }

    /**
     * Add [Patch]es and integrations to the [Patcher].
     *
     * @param patches The [Patch]es to add.
     * @param integrations The integrations to add. Must be a DEX file or container of DEX files.
     */
    @Suppress("NAME_SHADOWING")
    override fun accept(patches: Set<Patch<*>>, integrations: Set<File>) {
        // region Add patches

        // Add all patches to the executablePatches set.
        context.executablePatches.addAll(patches)

        // Add all patches and their dependencies to the allPatches set.
        patches.forEach { patch ->
            fun Patch<*>.addRecursively() =
                also(context.allPatches::add).dependencies.forEach(Patch<*>::addRecursively)

            patch.addRecursively()
        }

        // TODO: Detect circular dependencies.

        /**
         * Returns true if at least one patch or its dependencies matches the given predicate.
         *
         * @param predicate The predicate to match.
         */
        fun Patch<*>.anyRecursively(predicate: (Patch<*>) -> Boolean): Boolean =
            predicate(this) || dependencies.any { dependency -> dependency.anyRecursively(predicate) }

        context.allPatches.let { patches ->
            // Check, if what kind of resource mode is required.
            config.resourceMode = if (patches.any { patch -> patch.anyRecursively { it is ResourcePatch } }) {
                ResourcePatchContext.ResourceMode.FULL
            } else if (patches.any { patch -> patch.anyRecursively { it is RawResourcePatch } }) {
                ResourcePatchContext.ResourceMode.RAW_ONLY
            } else {
                ResourcePatchContext.ResourceMode.NONE
            }

            // Check, if integrations need to be merged.
            for (patch in patches) {
                if (patch.anyRecursively { it.requiresIntegrations }) {
                    context.bytecodeContext.integrations.merge = true
                    break
                }
            }
        }

        // endregion

        // region Add integrations

        context.bytecodeContext.integrations.addAll(integrations)

        // endregion
    }

    /**
     * Execute [Patch]es that were added to [Patcher].
     *
     * @param returnOnError If true, [Patcher] will return immediately if a [Patch] fails.
     * @return A pair of the name of the [Patch] and its [PatchResult].
     */
    override fun apply(returnOnError: Boolean) = flow {
        fun Patch<*>.execute(
            executedPatches: LinkedHashMap<Patch<*>, PatchResult>,
        ): PatchResult {
            // If the patch was executed before or failed, return it's the result.
            executedPatches[this]?.let { patchResult ->
                patchResult.exception ?: return patchResult

                return PatchResult(this, PatchException("The patch '$this' failed previously"))
            }

            // Recursively execute all dependency patches.
            dependencies.forEach { dependency ->
                dependency.execute(executedPatches).exception?.let {
                    return PatchResult(
                        this,
                        PatchException(
                            "The patch \"$this\" depends on \"$dependency\" which raised an exception:\n${it.stackTraceToString()}",
                        ),
                    )
                }
            }

            // Execute the patch.
            return try {
                execute(context)

                PatchResult(this)
            } catch (exception: PatchException) {
                PatchResult(this, exception)
            } catch (exception: Exception) {
                PatchResult(this, PatchException(exception))
            }.also { executedPatches[this] = it }
        }

        if (context.bytecodeContext.integrations.merge) context.bytecodeContext.integrations.flush()

        // Prevent from decoding the app manifest twice if it is not needed.
        if (config.resourceMode != ResourcePatchContext.ResourceMode.NONE) {
            context.resourceContext.decodeResources(config.resourceMode)
        }

        logger.info("Executing patches")

        val executedPatches = LinkedHashMap<Patch<*>, PatchResult>()

        context.executablePatches.sortedBy { it.name }.forEach { patch ->
            val patchResult = patch.execute(executedPatches)

            // If the patch failed, emit the result, even if it is closeable.
            // Results of executed patches that are closeable will be emitted later.
            patchResult.exception?.let {
                // Propagate exception to caller instead of wrapping it in a new exception.
                emit(patchResult)

                if (returnOnError) return@flow
            } ?: run {
                emit(patchResult)
            }
        }

        executedPatches.values.filter { it.exception == null }.asReversed().forEach { executionResult ->
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

            result.exception?.let {
                emit(
                    PatchResult(
                        patch,
                        PatchException(
                            "The patch \"$patch\" raised an exception: ${it.stackTraceToString()}",
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

    override fun close() = context.bytecodeContext.methodLookupMaps.close()

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
