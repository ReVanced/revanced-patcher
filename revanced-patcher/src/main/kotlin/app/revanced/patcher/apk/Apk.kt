@file:Suppress("MemberVisibilityCanBePrivate")

package app.revanced.patcher.apk

import app.revanced.arsc.ApkResourceException
import app.revanced.arsc.archive.Archive
import app.revanced.arsc.resource.ResourceContainer
import app.revanced.patcher.Patcher
import app.revanced.patcher.PatcherOptions
import app.revanced.patcher.logging.asArscLogger
import app.revanced.patcher.util.ProxyBackedClassList
import com.reandroid.apk.ApkModule
import com.reandroid.apk.xmlencoder.EncodeException
import com.reandroid.arsc.chunk.xml.AndroidManifestBlock
import com.reandroid.arsc.value.ResConfig
import lanchon.multidexlib2.*
import org.jf.dexlib2.Opcodes
import org.jf.dexlib2.dexbacked.DexBackedDexFile
import org.jf.dexlib2.iface.DexFile
import org.jf.dexlib2.iface.MultiDexContainer
import org.jf.dexlib2.writer.io.MemoryDataStore
import java.io.File

/**
 * An [Apk] file.
 */
sealed class Apk private constructor(module: ApkModule) {
    /**
     * A wrapper around the zip archive of this [Apk].
     *
     * @see Archive
     */
    private val archive = Archive(module)

    /**
     * The metadata of the [Apk].
     */
    val packageMetadata = PackageMetadata(module.androidManifestBlock)

    val resources = ResourceContainer(archive, module.tableBlock)

    /**
     * Refresh updated resources and close any open files.
     *
     * @param options The [PatcherOptions] of the [Patcher].
     */
    internal open fun cleanup(options: PatcherOptions) {
        try {
            archive.cleanup(options.logger.asArscLogger())
        } catch (e: EncodeException) {
            throw ApkResourceException.Encode(e.message!!, e)
        }

        resources.refreshPackageName()
    }

    /**
     * Write the [Apk] to a file.
     *
     * @param output The target file.
     */
    fun write(output: File) = archive.save(output)

    companion object {
        const val MANIFEST_FILE_NAME = "AndroidManifest.xml"

        /**
         * Determine the [Module] and [Type] of an [ApkModule].
         *
         * @return A [Pair] containing the [Module] and [Type] of the [ApkModule].
         */
        fun ApkModule.identify(): Pair<Module, Type> {
            val manifestElement = androidManifestBlock.manifestElement
            return when {
                isBaseModule -> Module.Main to Type.Base
                // The module is a base apk for a dynamic feature module if the "isFeatureModule" attribute is set to true.
                manifestElement.searchAttributeByName("isFeatureModule")?.valueAsBoolean == true -> Module.DynamicFeature(
                    split
                ) to Type.Base

                else -> {
                    val module = manifestElement.searchAttributeByName("configForSplit")
                    ?.let { Module.DynamicFeature(it.valueAsString) } ?: Module.Main

                    // Examples:
                    // config.xhdpi
                    // df_my_feature.config.en
                    val config = this.split.split(".").last()

                    val type = when {
                        // Language splits have a two-letter country code.
                        config.length == 2 -> Type.Language(config)
                        // Library splits use the target CPU architecture.
                        Split.Library.architectures.contains(config) -> Type.Library(config)
                        // Asset splits use the density.
                        ResConfig.Density.valueOf(config) != null -> Type.Asset(config)
                        else -> throw IllegalArgumentException("Invalid split config: $config")
                    }

                    module to type
                }
            }
        }
    }

    internal inner class BytecodeData {
        private val opcodes: Opcodes

        /**
         * The classes and proxied classes of the [Base] apk file.
         */
        val classes: ProxyBackedClassList

        init {
            MultiDexContainerBackedDexFile(object : MultiDexContainer<DexBackedDexFile> {
                // Load all dex files from the apk module and create a dex entry for each of them.
                private val entries = archive.readDexFiles()
                    .mapValues { (name, data) -> BasicDexEntry(this, name, RawDexIO.readRawDexFile(data, 0, null)) }

                override fun getDexEntryNames() = entries.keys.toList()
                override fun getEntry(entryName: String) = entries[entryName]
            }).let {
                opcodes = it.opcodes
                classes = ProxyBackedClassList(it.classes)
            }
        }

        /**
         * Write [classes] to the archive.
         */
        internal fun writeDexFiles() {
            // Create patched dex files.
            mutableMapOf<String, MemoryDataStore>().also {
                val newDexFile = object : DexFile {
                    override fun getClasses() =
                        this@BytecodeData.classes.also(ProxyBackedClassList::applyProxies).toSet()
                    override fun getOpcodes() = this@BytecodeData.opcodes
                }

                // Write modified dex files.
                MultiDexIO.writeDexFile(
                    true, -1, // Core count.
                    it, Patcher.dexFileNamer, newDexFile, DexIO.DEFAULT_MAX_DEX_POOL_SIZE, null
                )
            }.forEach { (name, store) ->
                archive.writeRaw(name, store.data)
            }
        }
    }

    /**
     * Metadata about an [Apk] file.
     *
     * @param packageName The package name of the [Apk] file.
     * @param packageVersion The package version of the [Apk] file.
     */
    data class PackageMetadata(val packageName: String?, val packageVersion: String?) {
        internal constructor(manifestBlock: AndroidManifestBlock) : this(
            manifestBlock.packageName,
            manifestBlock.versionName
        )
    }

    /**
     * An [Apk] of type [Split].
     *
     * @param config The device configuration associated with this [Split], such as arm64_v8a, en or xhdpi.
     * @see Apk
     */
    sealed class Split(val config: String, module: ApkModule) : Apk(module) {
        override fun toString() = "split_config.$config.apk"

        /**
         * The split apk file which contains libraries.
         *
         * @see Split
         */
        class Library internal constructor(config: String, module: ApkModule) : Split(config, module) {
            companion object {
                /**
                 * A set of all architectures supported by android.
                 */
                val architectures = setOf("armeabi_v7a", "arm64_v8a", "x86", "x86_64")
            }
        }

        /**
         * The split apk file which contains language strings.
         *
         * @see Split
         */
        class Language internal constructor(config: String, module: ApkModule) : Split(config, module)

        /**
         * The split apk file which contains assets.
         *
         * @see Split
         */
        class Asset internal constructor(config: String, module: ApkModule) : Split(config, module)
    }

    /**
     * The base [Apk] file..
     *
     * @see Apk
     */
    class Base internal constructor(module: ApkModule) : Apk(module) {
        /**
         * Data of the [Base] apk file.
         */
        internal val bytecodeData = BytecodeData()

        override fun toString() = "base.apk"

        override fun cleanup(options: PatcherOptions) {
            super.cleanup(options)

            options.logger.info("Writing patched dex files")
            bytecodeData.writeDexFiles()
        }
    }

    /**
     * The module that the [ApkModule] belongs to.
     */
    sealed class Module {
        /**
         * The default [Module] that is always installed by software repositories.
         */
        object Main : Module()

        /**
         * A [Module] that can be installed later by software repositories when requested by the application.
         *
         * @param name The name of the feature.
         */
        data class DynamicFeature(val name: String) : Module()
    }

    /**
     * The type of the [ApkModule].
     */
    sealed class Type {
        /**
         * The main Apk of a [Module].
         */
        object Base : Type()

        /**
         * A superclass for all split configuration types.
         *
         * @param target The target device configuration.
         */
        sealed class SplitConfig(val target: String) : Type()

        /**
         * The [Type] of an apk containing native libraries.
         *
         * @param architecture The target CPU architecture.
         */
        data class Library(val architecture: String) : SplitConfig(architecture)

        /**
         * The [Type] for an Apk containing language resources.
         *
         * @param language The target language code.
         */
        data class Language(val language: String) : SplitConfig(language)

        /**
         * The [Type] for an Apk containing assets.
         *
         * @param pixelDensity The target screen density.
         */
        data class Asset(val pixelDensity: String) : SplitConfig(pixelDensity)
    }
}
