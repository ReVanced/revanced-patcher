package app.revanced.patcher

import app.revanced.patcher.apk.Apk
import app.revanced.patcher.extensions.PatchExtensions.dependencies
import app.revanced.patcher.extensions.PatchExtensions.patchName
import app.revanced.patcher.extensions.PatchExtensions.requiresIntegrations
import app.revanced.patcher.fingerprint.method.impl.MethodFingerprint
import app.revanced.patcher.fingerprint.method.impl.MethodFingerprint.Companion.resolveUsingLookupMap
import app.revanced.patcher.patch.*
import app.revanced.patcher.util.VersionReader
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import lanchon.multidexlib2.BasicDexFileNamer
import java.io.Closeable
import java.io.File
import java.util.function.Function

typealias ExecutedPatchResults = Flow<Pair<String, PatchException?>>

/**
 * The ReVanced Patcher.
 * @param options The options for the patcher.
 * @param patches The patches to use.
 * @param integrations The integrations to merge if necessary. Must be dex files or dex file container such as ZIP, APK or DEX files.
 */
class Patcher(private val options: PatcherOptions, patches: Iterable<PatchClass>, integrations: Iterable<File>) :
    Function<Boolean, ExecutedPatchResults> {
    private val context = PatcherContext(options, patches.toList(), integrations)
    private val logger = options.logger

    companion object {
        /**
         * The version of the ReVanced Patcher.
         */
        @JvmStatic
        val version = VersionReader.read()

        @Suppress("SpellCheckingInspection")
        internal val dexFileNamer = BasicDexFileNamer()
    }

    init {
        /**
         * Returns true if at least one patches or its dependencies matches the given predicate.
         */
        fun PatchClass.anyRecursively(predicate: (PatchClass) -> Boolean): Boolean =
            predicate(this) || dependencies?.any { it.java.anyRecursively(predicate) } == true

        // Determine if merging integrations is required.
        for (patch in context.patches) {
            if (patch.anyRecursively { it.requiresIntegrations }) {
                context.integrations.merge = true
                break
            }
        }
    }

    /**
     * Execute the patcher.
     *
     * @param stopOnError If true, the patches will stop on the first error.
     * @return A pair of the name of the [Patch] and a [PatchException] if it failed.
     */
    override fun apply(stopOnError: Boolean) = flow {
        /**
         * Execute a [Patch] and its dependencies recursively.
         *
         * @param patchClass The [Patch] to execute.
         * @param executedPatches A map of [Patch]es paired to a boolean indicating their success, to prevent infinite recursion.
         */
        suspend fun executePatch(
            patchClass: PatchClass,
            executedPatches: HashMap<String, ExecutedPatch>
        ) {
            val patchName = patchClass.patchName

            // If the patch has already executed silently skip it.
            if (executedPatches.contains(patchName)) {
                if (!executedPatches[patchName]!!.success)
                    throw PatchException("'$patchName' did not succeed previously")

                logger.trace("Skipping '$patchName' because it has already been executed")

                return
            }

            // Recursively execute all dependency patches.
            patchClass.dependencies?.forEach { dependencyClass ->
                val dependency = dependencyClass.java

                try {
                    executePatch(dependency, executedPatches)
                } catch (throwable: Throwable) {
                    throw PatchException(
                        "'$patchName' depends on '${dependency.patchName}' " +
                                "but the following exception was raised: ${throwable.cause?.stackTraceToString() ?: throwable.message}",
                        throwable
                    )
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
                    bytecodePatch.fingerprints?.resolveUsingLookupMap(context.bytecodeContext)
                }
            }

            logger.trace("Executing '$patchName' of type: ${if (isResourcePatch) "resource" else "bytecode"}")

            var success = false
            try {
                patchInstance.execute(patchContext)

                success = true
            } catch (patchException: PatchException) {
                throw patchException
            } catch (throwable: Throwable) {
                throw PatchException("Unhandled patch exception: ${throwable.message}", throwable)
            } finally {
                executedPatches[patchName] = ExecutedPatch(patchInstance, success)
            }
        }

        if (context.integrations.merge) context.integrations.merge(logger)

        logger.trace("Initialize lookup maps for method MethodFingerprint resolution")

        MethodFingerprint.initializeFingerprintResolutionLookupMaps(context.bytecodeContext)

        logger.info("Executing patches")

        // Key is patch name.
        LinkedHashMap<String, ExecutedPatch>().apply {
            context.patches.forEach { patch ->
                var exception: PatchException? = null

                try {
                    executePatch(patch, this)
                } catch (patchException: PatchException) {
                    exception = patchException
                }

                // TODO: only emit if the patch is not a closeable.
                //  If it is a closeable, this should be done when closing the patch.
                emit(patch.patchName to exception)

                if (stopOnError && exception != null) return@flow
            }
        }.let {
            it.values
                .filter(ExecutedPatch::success)
                .map(ExecutedPatch::patchInstance)
                .filterIsInstance(Closeable::class.java)
                .asReversed().forEach { patch ->
                    try {
                        patch.close()
                    } catch (throwable: Throwable) {
                        val patchException =
                            if (throwable is PatchException) throwable
                            else PatchException(throwable)

                        val patchName = (patch as Patch<Context>).javaClass.patchName

                        logger.error("Failed to close '$patchName': ${patchException.stackTraceToString()}")

                        emit(patchName to patchException)

                        // This is not failsafe. If a patch throws an exception while closing,
                        // the other patches that depend on it may fail.
                        if (stopOnError) return@flow
                    }
                }
        }

        MethodFingerprint.clearFingerprintResolutionLookupMaps()
    }

    /**
     * Finish patching all [Apk]s.
     *
     * @return The [PatcherResult] of the [Patcher].
     */
    fun finish(): PatcherResult {
        val patchResults = buildList {
            logger.info("Processing patched apks")
            options.apkBundle.cleanup(options).forEach { result ->
                if (result.exception != null) {
                    logger.error("Got exception while processing ${result.apk}: ${result.exception.stackTraceToString()}")
                    return@forEach
                }

                val patch = result.let {
                    when (it.apk) {
                        is Apk.Base -> PatcherResult.Patch.Base(it.apk)
                        is Apk.Split -> PatcherResult.Patch.Split(it.apk)
                    }
                }

                add(patch)

                logger.info("Patched ${result.apk}")
            }
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