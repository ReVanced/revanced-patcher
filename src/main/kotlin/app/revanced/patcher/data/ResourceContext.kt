package app.revanced.patcher.data

import app.revanced.patcher.PatcherContext
import app.revanced.patcher.PatcherOptions
import app.revanced.patcher.util.DomFileEditor
import brut.androlib.AaptInvoker
import brut.androlib.ApkDecoder
import brut.androlib.apk.UsesFramework
import brut.androlib.res.Framework
import brut.androlib.res.ResourcesDecoder
import brut.androlib.res.decoder.AndroidManifestResourceParser
import brut.androlib.res.decoder.XmlPullStreamDecoder
import brut.androlib.res.xml.ResXmlPatcher
import brut.directory.ExtFile
import java.io.File
import java.io.InputStream
import java.io.OutputStream
import java.nio.file.Files
import java.util.logging.Logger

/**
 * A context for resources.
 * This holds the current state of the resources.
 *
 * @param context The [PatcherContext] to create the context for.
 */
class ResourceContext internal constructor(
    private val context: PatcherContext,
    private val options: PatcherOptions
) : Context<File?>, Iterable<File> {
    private val logger = Logger.getLogger(ResourceContext::class.java.name)

    val xmlEditor = XmlFileHolder()

    /**
     * Decode resources for the patcher.
     *
     * @param mode The [ResourceDecodingMode] to use when decoding.
     */
    internal fun decodeResources(mode: ResourceDecodingMode) = with(context.packageMetadata.apkInfo) {
        // Needed to decode resources.
        val resourcesDecoder = ResourcesDecoder(options.resourceConfig, this)

        when (mode) {
            ResourceDecodingMode.FULL -> {
                val outDir = options.recreateResourceCacheDirectory()

                logger.info("Decoding resources")

                resourcesDecoder.decodeResources(outDir)
                resourcesDecoder.decodeManifest(outDir)

                // Needed to record uncompressed files.
                val apkDecoder = ApkDecoder(options.resourceConfig, this)
                apkDecoder.recordUncompressedFiles(resourcesDecoder.resFileMapping)

                usesFramework = UsesFramework().apply {
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
                    apkFile.directory.getFileInput("AndroidManifest.xml"),
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
                    versionInfo.let {
                        metadata.packageVersion = it.versionName ?: it.versionCode
                    }
                }
            }
        }

    }

    operator fun get(path: String) = options.resourceCachePath.resolve(path)

    override fun iterator() = options.resourceCachePath.walkTopDown().iterator()


    /**
     * Compile resources from the [ResourceContext].
     *
     * @return The compiled resources.
     */
    override fun get(): File? {
        var resourceFile: File? = null

        if (options.resourceDecodingMode == ResourceDecodingMode.FULL) {
            logger.info("Compiling modified resources")

            val cacheDirectory = ExtFile(options.resourceCachePath)
            val aaptFile = cacheDirectory.resolve("aapt_temp_file").also {
                Files.deleteIfExists(it.toPath())
            }.also { resourceFile = it }

            try {
                AaptInvoker(
                    options.resourceConfig, context.packageMetadata.apkInfo
                ).invokeAapt(aaptFile,
                    cacheDirectory.resolve("AndroidManifest.xml").also {
                        ResXmlPatcher.fixingPublicAttrsInProviderAttributes(it)
                    },
                    cacheDirectory.resolve("res"),
                    null,
                    null,
                    context.packageMetadata.apkInfo.usesFramework.let { usesFramework ->
                        usesFramework.ids.map { id ->
                            Framework(options.resourceConfig).getFrameworkApk(id, usesFramework.tag)
                        }.toTypedArray()
                    })
            } finally {
                cacheDirectory.close()
            }
        }

        return resourceFile
    }

    /**
     * The type of decoding the resources.
     */
    internal enum class ResourceDecodingMode {
        /**
         * Decode all resources.
         */
        FULL,

        /**
         * Decode the manifest file only.
         */
        MANIFEST_ONLY,
    }

    inner class XmlFileHolder {
        operator fun get(inputStream: InputStream) =
            DomFileEditor(inputStream)

        operator fun get(path: String): DomFileEditor {
            return DomFileEditor(this@ResourceContext[path])
        }

    }
}