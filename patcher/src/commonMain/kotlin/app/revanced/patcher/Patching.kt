package app.revanced.patcher

import app.revanced.patcher.patch.*
import java.io.File
import java.io.InputStream
import java.io.deleteRecursively
import java.io.resolve
import java.util.logging.Logger

fun patcher(
    apkFile: File,
    temporaryFilesPath: File = File("revanced-patcher-temporary-files"),
    aaptBinaryPath: File? = null,
    frameworkFileDirectory: String? = null,
    getPatches: (packageName: String, versionName: String) -> Set<Patch>,
): (emit: (PatchResult) -> Unit) -> PatchesResult {
    val logger = Logger.getLogger("Patcher")

    if (temporaryFilesPath.exists()) {
        logger.info("Deleting existing temporary files directory")

        if (!temporaryFilesPath.deleteRecursively())
            logger.severe("Failed to delete existing temporary files directory")
    }

    val apkFilesPath = temporaryFilesPath.resolve("apk").also { it.mkdirs() }
    val patchedFilesPath = temporaryFilesPath.resolve("patched").also { it.mkdirs() }

    val resourcePatchContext = ResourcePatchContext(
        apkFile,
        apkFilesPath,
        patchedFilesPath,
        aaptBinaryPath,
        frameworkFileDirectory
    )

    val (packageName, versionName) = resourcePatchContext.decodeManifest()
    val patches = getPatches(packageName, versionName)

    return { emit: (PatchResult) -> Unit ->
        if (patches.any { patch -> patch.patchesResources }) resourcePatchContext.decodeResources()

        // After initializing the resource context, to keep memory usage time low.
        val bytecodePatchContext = BytecodePatchContext(
            apkFile,
            patchedFilesPath
        )

        logger.info("Warming up the cache")

        bytecodePatchContext.classDefs.initializeCache()

        logger.info("Applying patches")

        patches.apply(bytecodePatchContext, resourcePatchContext, emit)
    }
}

// Public for testing.
fun Set<Patch>.apply(
    bytecodePatchContext: BytecodePatchContext,
    resourcePatchContext: ResourcePatchContext,
    emit: (PatchResult) -> Unit
): PatchesResult {
    val appliedPatches = LinkedHashMap<Patch, PatchResult>()

    sortedBy { it.name }.forEach { patch ->
        fun Patch.apply(): PatchResult {
            val result = appliedPatches[this]

            return if (result == null) {
                val failedDependency = dependencies.asSequence().map { it.apply() }.firstOrNull { it.exception != null }
                if (failedDependency != null) return patchResult(
                    "The dependant patch \"$failedDependency\" of the patch \"$this\" raised an exception:\n" +
                            failedDependency.exception!!.stackTraceToString(),
                )

                val exception = runCatching { apply(bytecodePatchContext, resourcePatchContext) }
                    .exceptionOrNull() as? Exception

                patchResult(exception).also { result -> appliedPatches[this] = result }
            } else if (result.exception == null) result
            else patchResult("The patch '$this' has failed previously")
        }

        val patchResult = patch.apply()

        // If an exception occurred or the patch has no finalize block, emit the result.
        if (patchResult.exception != null || patch.afterDependents == null) {
            emit(patchResult)
        }
    }

    val succeededPatchesWithFinalizeBlock = appliedPatches.values.filter {
        it.exception == null && it.patch.afterDependents != null
    }

    succeededPatchesWithFinalizeBlock.asReversed().forEach { result ->
        val patch = result.patch
        runCatching { patch.afterDependents!!.invoke(bytecodePatchContext, resourcePatchContext) }.fold(
            { emit(result) },
            {
                emit(
                    PatchResult(
                        patch,
                        PatchException(
                            "The patch \"$patch\" raised an exception:\n" + it.stackTraceToString(),
                            it,
                        ),
                    )
                )
            }
        )
    }

    return PatchesResult(bytecodePatchContext.get(), resourcePatchContext.get())
}

/**
 * The result of applying patches.
 *
 * @param dexFiles The patched dex files.
 * @param resources The patched resources.
 */
class PatchesResult internal constructor(
    val dexFiles: Set<PatchedDexFile>,
    val resources: PatchedResources?,
) {

    /**
     * A dex file.
     *
     * @param name The original name of the dex file.
     * @param stream The dex file as [InputStream].
     */
    class PatchedDexFile internal constructor(val name: String, val stream: InputStream)

    /**
     * The resources of a patched apk.
     *
     * @param resourcesApk The compiled resources.apk file.
     * @param otherResources The directory containing other resources files.
     * @param doNotCompress List of files that should not be compressed.
     * @param deleteResources List of resources that should be deleted.
     */
    class PatchedResources internal constructor(
        val resourcesApk: File?,
        val otherResources: File?,
        val doNotCompress: Set<String>,
        val deleteResources: Set<String>,
    )
}
