package app.revanced.patcher.patch

import app.revanced.java.io.kmpResolve
import app.revanced.patcher.PatchesResult
import app.revanced.patcher.patch.ResourcePatchContext.ResourceDecodingMode.ALL
import app.revanced.patcher.util.Document
import brut.androlib.AaptInvoker
import brut.androlib.ApkDecoder
import brut.androlib.Config
import brut.androlib.apk.ApkInfo
import brut.androlib.apk.UsesFramework
import brut.androlib.res.Framework
import brut.androlib.res.ResourcesDecoder
import brut.androlib.res.decoder.AndroidManifestPullStreamDecoder
import brut.androlib.res.decoder.AndroidManifestResourceParser
import brut.androlib.res.xml.ResXmlUtils
import brut.directory.ExtFile
import java.io.File
import java.io.InputStream
import java.io.OutputStream
import java.nio.file.Files
import java.util.logging.Logger
import kotlin.reflect.jvm.jvmName

/**
 * A context for patches containing the current state of resources.
 *
 * @param apkFile The apk file to patch.
 * @param apkFilesPath The path to the temporary apk files directory.
 * @param patchedFilesPath The path to the temporary patched files directory.
 * @param aaptBinaryPath The path to a custom aapt binary.
 * @param frameworkFileDirectory The path to the directory to cache the framework file in.
 */
class ResourcePatchContext internal constructor(
    private val apkFile: File,
    private val apkFilesPath: File,
    private val patchedFilesPath: File,
    aaptBinaryPath: File? = null,
    frameworkFileDirectory: String? = null,
) : PatchContext<PatchesResult.PatchedResources?> {
    private val apkInfo = ApkInfo(ExtFile(apkFile))

    private val logger = Logger.getLogger(ResourcePatchContext::class.jvmName)

    private val resourceConfig =
        Config.getDefaultConfig().apply {
            aaptBinary = aaptBinaryPath
            frameworkDirectory = frameworkFileDirectory
        }

    private var decodingMode = ResourceDecodingMode.MANIFEST

    /**
     * Read a document from an [InputStream].
     */
    fun document(inputStream: InputStream) = Document(inputStream)

    /**
     * Read and write documents in the [apkFile].
     */
    fun document(path: String) = Document(get(path))

    /**
     * Set of resources from [apkFile] to delete.
     */
    private val deleteResources = mutableSetOf<String>()

    internal fun decodeManifest(): Pair<PackageName, VersionName> {
        logger.info("Decoding manifest")

        val resourcesDecoder = ResourcesDecoder(resourceConfig, apkInfo)

        // Decode manually instead of using resourceDecoder.decodeManifest
        // because it does not support decoding to an OutputStream.
        AndroidManifestPullStreamDecoder(
            AndroidManifestResourceParser(resourcesDecoder.resTable),
            resourcesDecoder.newXmlSerializer(),
        ).decode(
            apkInfo.apkFile.directory.getFileInput("AndroidManifest.xml"),
            // Older Android versions do not support OutputStream.nullOutputStream()
            object : OutputStream() {
                override fun write(b: Int) { // Do nothing.
                }
            },
        )

        // Get the package name and version from the manifest using the XmlPullStreamDecoder.
        // The call to AndroidManifestPullStreamDecoder.decode() above sets apkInfo.
        val packageName = resourcesDecoder.resTable.packageRenamed
        val packageVersion =
            apkInfo.versionInfo.versionName ?: apkInfo.versionInfo.versionCode

        /*
         When the main resource package is not loaded, the ResTable is flagged as sparse.
         Because ResourcesDecoder.decodeResources loads the main package and is not called here,
         set sparseResources to false again to prevent the ResTable from being flagged as sparse falsely,
         in case ResourcesDecoder.decodeResources is not later used in the patching process
          to set sparseResources correctly.

         See ARSCDecoder.readTableType for more info.
         */
        apkInfo.sparseResources = false

        return packageName to packageVersion
    }

    internal fun decodeResources() {
        logger.info("Decoding resources")

        decodingMode = ALL

        val resourcesDecoder =
            ResourcesDecoder(resourceConfig, apkInfo).also {
                it.decodeResources(apkFilesPath)
                it.decodeManifest(apkFilesPath)
            }

        // Record uncompressed files to preserve their state when recompiling.
        ApkDecoder(apkInfo, resourceConfig).recordUncompressedFiles(resourcesDecoder.resFileMapping)

        // Get the ids of the used framework packages to include them for reference when recompiling.
        apkInfo.usesFramework =
            UsesFramework().apply {
                ids = resourcesDecoder.resTable.listFramePackages().map { it.id }
            }
    }

    /**
     * Compile resources in [apkFilesPath].
     *
     * @return The [PatchesResult.PatchedResources].
     */
    override fun get(): PatchesResult.PatchedResources {
        logger.info("Compiling patched resources")

        val resourcesPath = patchedFilesPath.kmpResolve("resources").also { it.mkdirs() }

        val resourcesApkFile =
            if (decodingMode == ResourceDecodingMode.ALL) {
                val resourcesApkFile = resourcesPath.kmpResolve("resources.apk").also { it.createNewFile() }

                val manifestFile =
                    apkFilesPath.kmpResolve("AndroidManifest.xml").also(ResXmlUtils::fixingPublicAttrsInProviderAttributes)

                val resPath = apkFilesPath.kmpResolve("res")
                val frameworkApkFiles =
                    with(Framework(resourceConfig)) {
                        apkInfo.usesFramework.ids.map { id -> getFrameworkApk(id, null) }
                    }.toTypedArray()

                AaptInvoker(
                    resourceConfig,
                    apkInfo,
                ).invoke(resourcesApkFile, manifestFile, resPath, null, null, frameworkApkFiles)

                resourcesApkFile
            } else {
                null
            }

        val otherFiles =
            apkFilesPath.listFiles()!!.filter {
                // Excluded because present in resources.other.
                // TODO: We are reusing apkFiles as a temporarily directory for extracting resources.
                //  This is not ideal as it could conflict with files such as the ones that are filtered here.
                //  The problem is that ResourcePatchContext#get returns a File relative to apkFiles,
                //  and we need to extract files to that directory.
                //  A solution would be to use apkFiles as the working directory for the patching process.
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
                resourcesPath.kmpResolve("other").also { it.mkdirs() }.apply {
                    otherFiles.forEach { file ->
                        Files.move(file.toPath(), kmpResolve(file.name).toPath())
                    }
                }
            } else {
                null
            }

        return PatchesResult.PatchedResources(
            resourcesApkFile,
            otherResourceFiles,
            apkInfo.doNotCompress?.toSet() ?: emptySet(),
            deleteResources,
        )
    }

    /**
     * Get a file from [apkFilesPath].
     *
     * @param path The path of the file.
     * @param copy Whether to copy the file from [apkFile] if it does not exist yet in [apkFilesPath].
     */
    operator fun get(
        path: String,
        copy: Boolean = true,
    ) = apkFilesPath.kmpResolve(path).apply {
        if (copy && !exists()) {
            with(ExtFile(apkFile).directory) {
                if (containsFile(path) || containsDir(path)) {
                    copyToDir(apkFilesPath, path)
                }
            }
        }
    }

    /**
     * Mark a file for deletion when the APK is rebuilt.
     *
     * @param name The name of the file to delete.
     */
    fun delete(name: String) = deleteResources.add(name)

    /**
     * How to handle resources decoding and compiling.
     */
    internal enum class ResourceDecodingMode {
        /**
         * Decode and compile all resources.
         */
        ALL,

        /**
         * Do not decode or compile any resources.
         */
        NONE,

        /**
         * Do not decode or compile any resources.
         */
        MANIFEST,
    }
}
