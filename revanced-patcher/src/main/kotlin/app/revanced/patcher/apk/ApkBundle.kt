@file:Suppress("MemberVisibilityCanBePrivate")

package app.revanced.patcher.apk

import app.revanced.arsc.ApkResourceException
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
class ApkBundle(files: List<File>) : Sequence<Apk> {
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
     * The [ResourceTable] of this [ApkBundle].
     */
    val resources = ResourceTable(base.resources, map { it.resources })

    override fun iterator() = sequence {
        yield(base)
        splits?.values?.let {
            yieldAll(it)
        }
    }.iterator()

    /**
     * Refresh all updated resources in an [ApkBundle].
     *
     * @param options The [PatcherOptions] of the [Patcher].
     * @return A sequence of the [Apk] files which are being refreshed.
     */
    internal fun cleanup(options: PatcherOptions) = map {
        var exception: ApkResourceException? = null
        try {
            it.cleanup(options)
        } catch (e: ApkResourceException) {
            exception = e
        }

        SplitApkResult(it, exception)
    }

    /**
     * The result of writing an [Apk] file.
     *
     * @param apk The corresponding [Apk] file.
     * @param exception The optional [ApkResourceException] when an exception occurred.
     */
    data class SplitApkResult(val apk: Apk, val exception: ApkResourceException? = null)
}