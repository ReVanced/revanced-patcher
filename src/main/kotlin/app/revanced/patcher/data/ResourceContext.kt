package app.revanced.patcher.data

import app.revanced.patcher.InternalApi
import app.revanced.patcher.PackageMetadata
import app.revanced.patcher.PatcherConfig
import app.revanced.patcher.PatcherResult
import app.revanced.patcher.util.Document
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
 * A context for the patcher containing the current state of the resources.
 *
 * @param packageMetadata The [PackageMetadata] of the apk file.
 * @param config The [PatcherConfig] used to create this context.
 */
class ResourceContext internal constructor(
    private val packageMetadata: PackageMetadata,
    private val config: PatcherConfig,
) : Context<PatcherResult.PatchedResources?>, Iterable<File> {
    private val logger = Logger.getLogger(ResourceContext::class.java.name)

    /**
     * Read and write documents in the [PatcherConfig.apkFiles].
     */
    val document = DocumentOperatable()

    @Deprecated("Use document instead.")
    val xmlEditor = XmlFileHolder()

    /**
     * Predicate to delete resources from [PatcherConfig.apkFiles].
     */
    private val deleteResources = mutableSetOf<(String) -> Boolean>()

    /**
     * Decode resources of [PatcherConfig.apkFile].
     *
     * @param mode The [ResourceMode] to use.
     */
    internal fun decodeResources(mode: ResourceMode) =
        with(packageMetadata.apkInfo) {
            config.initializeTemporaryFilesDirectories()

            // Needed to decode resources.
            val resourcesDecoder = ResourcesDecoder(config.resourceConfig, this)

            if (mode == ResourceMode.FULL) {
                logger.info("Decoding resources")

                resourcesDecoder.decodeResources(config.apkFiles)
                resourcesDecoder.decodeManifest(config.apkFiles)

                // Needed to record uncompressed files.
                val apkDecoder = ApkDecoder(config.resourceConfig, this)
                apkDecoder.recordUncompressedFiles(resourcesDecoder.resFileMapping)

                usesFramework =
                    UsesFramework().apply {
                        ids = resourcesDecoder.resTable.listFramePackages().map { it.id }
                    }
            } else {
                logger.info("Decoding app manifest")

                // Decode manually instead of using resourceDecoder.decodeManifest
                // because it does not support decoding to an OutputStream.
                XmlPullStreamDecoder(
                    AndroidManifestResourceParser(resourcesDecoder.resTable),
                    resourcesDecoder.resXmlSerializer,
                ).decodeManifest(
                    apkFile.directory.getFileInput("AndroidManifest.xml"),
                    // Older Android versions do not support OutputStream.nullOutputStream()
                    object : OutputStream() {
                        override fun write(b: Int) { // Do nothing.
                        }
                    },
                )

                // Get the package name and version from the manifest using the XmlPullStreamDecoder.
                // XmlPullStreamDecoder.decodeManifest() sets metadata.apkInfo.
                packageMetadata.let { metadata ->
                    metadata.packageName = resourcesDecoder.resTable.packageRenamed
                    versionInfo.let {
                        metadata.packageVersion = it.versionName ?: it.versionCode
                    }

                    /*
                     The ResTable if flagged as sparse if the main package is not loaded, which is the case here,
                     because ResourcesDecoder.decodeResources loads the main package
                     and not XmlPullStreamDecoder.decodeManifest.
                     See ARSCDecoder.readTableType for more info.

                     Set this to false again to prevent the ResTable from being flagged as sparse falsely.
                     */
                    metadata.apkInfo.sparseResources = false
                }
            }
        }

    /**
     * Compile resources in [PatcherConfig.apkFiles].
     *
     * @return The [PatcherResult.PatchedResources].
     */
    @InternalApi
    override fun get(): PatcherResult.PatchedResources? {
        if (config.resourceMode == ResourceMode.NONE) return null

        logger.info("Compiling modified resources")

        val resources = config.patchedFiles.resolve("resources").also { it.mkdirs() }

        val resourcesApkFile =
            if (config.resourceMode == ResourceMode.FULL) {
                resources.resolve("resources.apk").apply {
                    // Compile the resources.apk file.
                    AaptInvoker(
                        config.resourceConfig,
                        packageMetadata.apkInfo,
                    ).invokeAapt(
                        resources.resolve("resources.apk"),
                        config.apkFiles.resolve("AndroidManifest.xml").also {
                            ResXmlPatcher.fixingPublicAttrsInProviderAttributes(it)
                        },
                        config.apkFiles.resolve("res"),
                        null,
                        null,
                        packageMetadata.apkInfo.usesFramework.let { usesFramework ->
                            usesFramework.ids.map { id ->
                                Framework(config.resourceConfig).getFrameworkApk(id, usesFramework.tag)
                            }.toTypedArray()
                        },
                    )
                }
            } else {
                null
            }

        val otherFiles =
            config.apkFiles.listFiles()!!.filter {
                // Excluded because present in resources.other.
                // TODO: We are reusing config.apkFiles as a temporarily directory for extracting resources.
                //  This is not ideal as it could conflict with files such as the ones that we filter here.
                //  The problem is that ResourceContext#get returns a File relative to config.apkFiles,
                //  and we need to extract files to that directory.
                //  A solution would be to use config.apkFiles as the working directory for the patching process.
                //  Once all patches have been executed, we can move the decoded resources to a new directory.
                //  The filters wouldn't be needed anymore.
                //  For now, we assume that the files we filter here are not needed for the patching process.
                it.name != "AndroidManifest.xml" &&
                    it.name != "res" &&
                    // Generated by Androlib.
                    it.name != "build"
            }

        val otherResourceFiles =
            if (otherFiles.isNotEmpty()) {
                // Move the other resources files.
                resources.resolve("other").also { it.mkdirs() }.apply {
                    otherFiles.forEach { file ->
                        Files.move(file.toPath(), resolve(file.name).toPath())
                    }
                }
            } else {
                null
            }

        return PatcherResult.PatchedResources(
            resourcesApkFile,
            otherResourceFiles,
            packageMetadata.apkInfo.doNotCompress?.toSet() ?: emptySet(),
            deleteResources,
        )
    }

    /**
     * Get a file from [PatcherConfig.apkFiles].
     *
     * @param path The path of the file.
     * @param copy Whether to copy the file from [PatcherConfig.apkFile] if it does not exist yet in [PatcherConfig.apkFiles].
     */
    operator fun get(
        path: String,
        copy: Boolean = true,
    ) = config.apkFiles.resolve(path).apply {
        if (copy && !exists()) {
            with(ExtFile(config.apkFile).directory) {
                if (containsFile(path) || containsDir(path)) {
                    copyToDir(config.apkFiles, path)
                }
            }
        }
    }

    /**
     * Stage a file to be deleted from [PatcherConfig.apkFile].
     *
     * @param shouldDelete The predicate to stage the file for deletion given its name.
     */
    fun stageDelete(shouldDelete: (String) -> Boolean) = deleteResources.add(shouldDelete)

    @Deprecated("Use get(String, Boolean) instead.", ReplaceWith("get(path, false)"))
    operator fun get(path: String) = get(path, false)

    @Deprecated("Use get(String, Boolean) instead.")
    override fun iterator(): Iterator<File> = config.apkFiles.listFiles()!!.iterator()

    /**
     * How to handle resources decoding and compiling.
     */
    internal enum class ResourceMode {
        /**
         * Decode and compile all resources.
         */
        FULL,

        /**
         * Only extract resources from the APK.
         * The AndroidManifest.xml and resources inside /res are not decoded or compiled.
         */
        RAW_ONLY,

        /**
         * Do not decode or compile any resources.
         */
        NONE,
    }

    inner class DocumentOperatable {
        operator fun get(inputStream: InputStream) = Document(inputStream)

        operator fun get(path: String) = Document(this@ResourceContext[path])
    }

    @Deprecated("Use DocumentOperatable instead.")
    inner class XmlFileHolder {
        operator fun get(inputStream: InputStream) = DomFileEditor(inputStream)

        operator fun get(path: String): DomFileEditor {
            return DomFileEditor(this@ResourceContext[path])
        }
    }
}
