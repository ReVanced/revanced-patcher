package app.revanced.patcher.apk

import app.revanced.arsc.ApkException
import app.revanced.arsc.resource.ResourceTable
import app.revanced.patcher.Patcher
import app.revanced.patcher.PatcherOptions
import app.revanced.patcher.apk.Apk.Companion.identify
import com.reandroid.apk.ApkModule
import java.io.File

/**
 * An [Apk] file of type [Apk.Split].
 *
 * @param files A list of apk files to load.
 */
class ApkBundle(files: List<File>) {
    /**
     * The [Apk.Base] of this [ApkBundle].
     */
    val base: Apk.Base

    /**
     * A map containing all the [Apk.Split]s in this bundle associated by their configuration.
     */
    val splits: Map<String, Apk.Split>?

    init {
        var baseApk: Apk.Base? = null

        splits = buildMap {
            files.forEach {
                val apk = ApkModule.loadApkFile(it)
                val (module, type) = apk.identify()
                if (module is Apk.Module.DynamicFeature) {
                    return@forEach // Dynamic feature modules are not supported yet.
                }

                when (type) {
                    Apk.Type.Base -> {
                        if (baseApk != null) {
                            throw IllegalArgumentException("Cannot have more than one base apk")
                        }
                        baseApk = Apk.Base(apk)
                    }

                    is Apk.Type.SplitConfig -> {
                        val target = type.target
                        if (this.contains(target)) {
                            throw IllegalArgumentException("Duplicate split: $target")
                        }

                        val constructor = when (type) {
                            is Apk.Type.Asset -> Apk.Split::Asset
                            is Apk.Type.Library -> Apk.Split::Library
                            is Apk.Type.Language -> Apk.Split::Language
                        }

                        this[target] = constructor(target, apk)
                    }
                }
            }
        }.takeIf { it.isNotEmpty() }

        base = baseApk ?: throw IllegalArgumentException("Base apk not found")
    }

    /**
     * A [Sequence] yielding all [Apk]s in this [ApkBundle].
     */
    val all = sequence {
        yield(base)
        splits?.values?.let {
            yieldAll(it)
        }
    }

    /**
     * Get the [app.revanced.arsc.resource.ResourceContainer] for the specified configuration.
     *
     * @param config The config to search for.
     */
    fun query(config: String) = splits?.get(config)?.resources ?: base.resources

    /**
     * Refresh all updated resources in an [ApkBundle].
     *
     * @param options The [PatcherOptions] of the [Patcher].
     * @return A sequence of the [Apk] files which are being refreshed.
     */
    internal fun cleanup(options: PatcherOptions) = all.map {
        var exception: ApkException? = null
        try {
            it.cleanup(options)
        } catch (e: ApkException) {
            exception = e
        }

        SplitApkResult(it, exception)
    }

    /**
     * The [ResourceTable] of this [ApkBundle].
     */
    val resources = ResourceTable(base.resources, all.map { it.resources })

    /**
     * The result of writing an [Apk] file.
     *
     * @param apk The corresponding [Apk] file.
     * @param exception The optional [ApkException] when an exception occurred.
     */
    data class SplitApkResult(val apk: Apk, val exception: ApkException? = null)
}