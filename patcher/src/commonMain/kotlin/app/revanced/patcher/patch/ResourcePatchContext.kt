package app.revanced.patcher.patch

import app.revanced.java.io.kmpResolve
import app.revanced.patcher.Apk
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
 * @param apk The [Apk] to patch.
 * @param apkFilesPath The path to the temporary base APK files directory.
 * @param splitApkFilesPath The path to the temporary split APK files directory.
 * @param patchedFilesPath The path to the temporary patched files directory.
 * @param aaptBinaryPath The path to a custom aapt binary.
 * @param frameworkFileDirectory The path to the directory to cache the framework file in.
 */
class ResourcePatchContext internal constructor(
    private val apk: Apk,
    private val apkFilesPath: File,
    private val splitApkFilesPath: File,
    private val patchedFilesPath: File,
    aaptBinaryPath: File? = null,
    frameworkFileDirectory: String? = null,
) : PatchContext<PatchesResult.PatchedResources?> {
    private val apkInfo = ApkInfo(ExtFile(apk.file))

    private val logger = Logger.getLogger(ResourcePatchContext::class.jvmName)

    private val resourceConfig =
        Config.getDefaultConfig().apply {
            aaptBinary = aaptBinaryPath
            frameworkDirectory = frameworkFileDirectory
        }

    private var decodingMode = ResourceDecodingMode.MANIFEST

    private val splitApkInfos: Map<String, ApkInfo> =
        if (apk is Apk.Split) {
            apk.splitApkFiles.mapValues { (_, file) -> ApkInfo(ExtFile(file)) }
        } else {
            emptyMap()
        }

    private val splitDecodingModes: MutableMap<String, ResourceDecodingMode> =
        splitApkInfos.keys.associateWithTo(mutableMapOf()) { ResourceDecodingMode.MANIFEST }

    /**
     * Read a document from an [InputStream].
     */
    fun document(inputStream: InputStream) = Document(inputStream)

    /**
     * Read and write documents in the base APK.
     */
    fun document(path: String) = Document(get(path))

    /**
     * Set of resources from the base APK to delete.
     */
    private val deleteResources = mutableSetOf<String>()

    /**
     * Per-split sets of resources to delete.
     */
    private val splitDeleteResources: MutableMap<String, MutableSet<String>> = mutableMapOf()

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
                it.loadAuxiliaryPkgs(
                    splitApkInfos.values
                        .asSequence()
                        .filter { splitApkInfo -> splitApkInfo.apkFile.directory.containsFile("resources.arsc") }
                        .map { splitApkInfo -> splitApkInfo.apkFile }
                        .toList(),
                )
                it.setIncludeAuxiliaryPublicXml(true)
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

        for ((splitName, splitApkInfo) in splitApkInfos) {
            val splitFilesPath = splitApkFilesPath.kmpResolve(splitName).also { it.mkdirs() }

            // ABI splits contain only native libraries and no resource table.
            // Decode only the manifest so its package attribute can be synced with the base.
            if (!splitApkInfo.apkFile.directory.containsFile("resources.arsc")) {
                ResourcesDecoder(resourceConfig, splitApkInfo).decodeManifest(splitFilesPath)
                splitApkInfo.usesFramework = apkInfo.usesFramework
                continue
            }

            logger.info("Decoding resources for split \"$splitName\"")

            splitDecodingModes[splitName] = ALL

            val splitResourcesDecoder =
                ResourcesDecoder(resourceConfig, splitApkInfo).also {
                    it.decodeResources(splitFilesPath)
                    it.decodeManifest(splitFilesPath)
                }

            ApkDecoder(splitApkInfo, resourceConfig).recordUncompressedFiles(splitResourcesDecoder.resFileMapping)

            splitApkInfo.usesFramework =
                UsesFramework().apply {
                    ids = splitResourcesDecoder.resTable.listFramePackages().map { it.id }
                }
        }

        /*
         In split APKs, the base APK's resource table references drawables and values
         that physically reside in density/config splits. Without merging these into
         the base decode directory, AAPT fails to link resources during compilation.

         Symlinks are used to avoid duplicating files.
         */
        mergeSplitResources()
    }

    private fun mergeSplitResources() {
        val baseResDir = apkFilesPath.kmpResolve("res")
        if (!baseResDir.exists()) return

        for ((splitName, _) in splitApkInfos) {
            val splitResDir = splitApkFilesPath.kmpResolve(splitName).kmpResolve("res")
            if (!splitResDir.exists()) continue

            val resDirs = splitResDir.listFiles() ?: continue
            for (resTypeDir in resDirs) {
                if (!resTypeDir.isDirectory) continue

                val targetTypeDir = baseResDir.kmpResolve(resTypeDir.name)
                targetTypeDir.mkdirs()

                val files = resTypeDir.listFiles() ?: continue
                for (file in files) {
                    val targetFile = targetTypeDir.kmpResolve(file.name)
                    if (!targetFile.exists()) {
                        runCatching {
                            Files.createSymbolicLink(targetFile.toPath(), file.toPath().toAbsolutePath())
                        }.getOrElse {
                            file.copyTo(targetFile)
                        }
                    }
                }
            }
        }
    }

    /**
     * Ensure each split manifest's package attribute matches the base manifest.
     */
    private fun syncSplitPackageNames() {
        if (splitApkInfos.isEmpty()) return

        val baseManifest = apkFilesPath.kmpResolve("AndroidManifest.xml")
        if (!baseManifest.exists()) return

        val basePackageName = Document(baseManifest).use { document ->
            document.getElementsByTagName("manifest").item(0)
                ?.attributes?.getNamedItem("package")?.textContent
        } ?: return

        for ((splitName, _) in splitApkInfos) {
            val splitManifest = splitApkFilesPath.kmpResolve(splitName).kmpResolve("AndroidManifest.xml")
            if (!splitManifest.exists()) continue

            Document(splitManifest).use { document ->
                document.getElementsByTagName("manifest").item(0)
                    ?.attributes?.getNamedItem("package")?.let { it.textContent = basePackageName }
            }
        }
    }

    /**
     * Compile resources.
     *
     * @return The base [PatchesResult.PatchedResources] and per-split resources.
     */
    override fun get() = getCompiledResources().first

    internal fun getCompiledResources(): Pair<PatchesResult.PatchedResources, Map<String, PatchesResult.PatchedResources>> {
        logger.info("Compiling patched resources")

        // Android requires all splits to have the same package as the base.
        syncSplitPackageNames()

        val baseResources = compileResources(
            apkInfo = apkInfo,
            decodedResourcesPath = apkFilesPath,
            outputPath = patchedFilesPath.kmpResolve("resources").also { it.mkdirs() },
            decodingMode = decodingMode,
            deleteResources = deleteResources,
        )

        val splitResources = compileSplitResources()

        return baseResources to splitResources
    }

    private fun compileSplitResources(): Map<String, PatchesResult.PatchedResources> {
        if (splitApkInfos.isEmpty()) return emptyMap()

        return buildMap {
            for ((splitName, splitApkInfo) in splitApkInfos) {
                val decodedResourcesPath = splitApkFilesPath.kmpResolve(splitName)

                // Splits without resources.arsc were not decoded and have no output directory.
                if (!decodedResourcesPath.kmpResolve("res").exists() &&
                    !decodedResourcesPath.kmpResolve("AndroidManifest.xml").exists()
                ) continue

                logger.info("Compiling patched resources for split \"$splitName\"")

                put(splitName, compileResources(
                    apkInfo = splitApkInfo,
                    decodedResourcesPath = decodedResourcesPath,
                    outputPath = patchedFilesPath.kmpResolve("splits").kmpResolve(splitName).also { it.mkdirs() },
                    decodingMode = splitDecodingModes[splitName] ?: ResourceDecodingMode.MANIFEST,
                    deleteResources = splitDeleteResources[splitName] ?: emptySet(),
                ))
            }
        }
    }

    /**
     * Compile decoded resources from [decodedResourcesPath] into [outputPath].
     *
     * @param apkInfo The [ApkInfo] for the APK being compiled.
     * @param decodedResourcesPath The path containing decoded resources.
     * @param outputPath The path to write compiled output to.
     * @param decodingMode How resources were decoded, determining compilation behavior.
     * @param deleteResources Resources to delete from the APK.
     */
    private fun compileResources(
        apkInfo: ApkInfo,
        decodedResourcesPath: File,
        outputPath: File,
        decodingMode: ResourceDecodingMode,
        deleteResources: Set<String>,
    ): PatchesResult.PatchedResources {
        val resourcesApkFile =
            if (decodingMode != ResourceDecodingMode.NONE) {
                val manifestFile = decodedResourcesPath.kmpResolve("AndroidManifest.xml")
                if (!manifestFile.exists()) {
                    null
                } else {
                    val resourcesApkFile = outputPath.kmpResolve("resources.apk").also { it.createNewFile() }

                    manifestFile.also(ResXmlUtils::fixingPublicAttrsInProviderAttributes)

                    // Pass null for resPath when only the manifest was decoded,
                    // which makes AAPT compile only the manifest to binary XML.
                    val resPath = decodedResourcesPath.kmpResolve("res").takeIf { decodingMode == ALL }
                    val frameworkApkFiles =
                        with(Framework(resourceConfig)) {
                            apkInfo.usesFramework?.ids?.map { id -> getFrameworkApk(id, null) } ?: emptyList()
                        }.toTypedArray()

                    AaptInvoker(
                        resourceConfig,
                        apkInfo,
                    ).invoke(resourcesApkFile, manifestFile, resPath, null, null, frameworkApkFiles)

                    resourcesApkFile
                }
            } else {
                null
            }

        val otherFiles =
            decodedResourcesPath.listFiles()?.filter {
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
            } ?: emptyList()

        val otherResourceFiles =
            if (otherFiles.isNotEmpty()) {
                outputPath.kmpResolve("other").also { it.mkdirs() }.apply {
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
     * Get a file from the base APK's [apkFilesPath].
     *
     * @param path The path of the file.
     * @param copy Whether to copy the file from the base APK if it does not exist yet in [apkFilesPath].
     */
    operator fun get(
        path: String,
        copy: Boolean = true,
    ) = apkFilesPath.kmpResolve(path).apply {
        if (copy && !exists()) {
            with(ExtFile(apk.file).directory) {
                if (containsFile(path) || containsDir(path)) {
                    copyToDir(apkFilesPath, path)
                }
            }
        }
    }

    /**
     * Get a file from a split APK.
     *
     * @param splitName The name of the split.
     * @param path The path of the file within the split.
     * @param copy Whether to copy the file from the split APK if it does not exist yet.
     */
    fun split(
        splitName: String,
        path: String,
        copy: Boolean = true,
    ): File {
        val splitApkInfo = splitApkInfos[splitName]
            ?: throw PatchException("Split \"$splitName\" not found")

        val splitFilesPath = splitApkFilesPath.kmpResolve(splitName)

        return splitFilesPath.kmpResolve(path).apply {
            if (copy && !exists()) {
                with(splitApkInfo.apkFile.directory) {
                    if (containsFile(path) || containsDir(path)) {
                        copyToDir(splitFilesPath, path)
                    }
                }
            }
        }
    }

    /**
     * Read and write documents in a split APK.
     *
     * @param splitName The name of the split.
     * @param path The path of the document within the split.
     */
    fun splitDocument(splitName: String, path: String) = Document(split(splitName, path))

    /**
     * Mark a file for deletion from the base APK when it is rebuilt.
     *
     * @param name The name of the file to delete.
     */
    fun delete(name: String) = deleteResources.add(name)

    /**
     * Mark a file for deletion from a split APK when it is rebuilt.
     *
     * @param splitName The name of the split.
     * @param name The name of the file to delete.
     */
    fun deleteSplit(splitName: String, name: String) {
        if (splitName !in splitApkInfos) {
            throw PatchException("Split \"$splitName\" not found")
        }

        splitDeleteResources.getOrPut(splitName) { mutableSetOf() }.add(name)
    }

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
