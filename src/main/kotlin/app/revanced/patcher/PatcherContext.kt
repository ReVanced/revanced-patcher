package app.revanced.patcher

import app.revanced.patcher.patch.BytecodePatchContext
import app.revanced.patcher.patch.Patch
import app.revanced.patcher.patch.ResourcePatchContext
import brut.androlib.apk.ApkInfo
import brut.directory.ExtFile
import java.io.Closeable
import lanchon.multidexlib2.EmptyMultiDexContainerException
import java.util.logging.Logger

/**
 * A context for the patcher containing the current state of the patcher.
 *
 * @param config The configuration for the patcher.
 */
@Suppress("MemberVisibilityCanBePrivate")
class PatcherContext internal constructor(config: PatcherConfig): Closeable {
    private val logger = Logger.getLogger(this::class.java.name)

    /**
     * [PackageMetadata] of the supplied [PatcherConfig.apkFile].
     */
    val packageMetadata = PackageMetadata(ApkInfo(ExtFile(config.apkFile)))

    /**
     * The set of [Patch]es.
     */
    internal val executablePatches = mutableSetOf<Patch<*>>()

    /**
     * The set of all [Patch]es and their dependencies.
     */
    internal val allPatches = mutableSetOf<Patch<*>>()

    /**
     * The context for patches containing the current state of the resources.
     */
    internal val resourceContext = ResourcePatchContext(packageMetadata, config)

    /**
     * The context for patches containing the current state of the bytecode.
     */
    internal val bytecodeContext : BytecodePatchContext? = try {
        BytecodePatchContext(config)
    } catch (_: EmptyMultiDexContainerException) {
        logger.info("The APK contains no DEX files. Skipping bytecode patches")
        null
    }

    override fun close() = bytecodeContext?.close() ?: Unit
}
