@file:Suppress("unused")

package app.revanced.patcher

import app.revanced.patcher.extensions.AnnotationExtensions.findAnnotationRecursively
import app.revanced.patcher.patch.Patch
import app.revanced.patcher.patch.PatchClass
import dalvik.system.PathClassLoader
import lanchon.multidexlib2.BasicDexFileNamer
import lanchon.multidexlib2.MultiDexIO
import java.io.File
import java.net.URLClassLoader
import java.util.jar.JarFile

/**
 * A patch bundle.
 *
 * @param fromClasses The classes to get [Patch]es from.
 */
sealed class PatchBundleLoader private constructor(
    fromClasses: Iterable<Class<*>>
) : MutableList<PatchClass> by mutableListOf() {
    init {
        fromClasses.filter {
            if (it.isAnnotation) return@filter false

            it.findAnnotationRecursively(app.revanced.patcher.patch.annotations.Patch::class) != null
        }.map {
            @Suppress("UNCHECKED_CAST")
            it as PatchClass
        }.let { addAll(it) }
    }

    /**
     * A [PatchBundleLoader] for JAR files.
     *
     * @param patchBundles The path to patch bundles of JAR format.
     */
    class Jar(private vararg val patchBundles: File) : PatchBundleLoader(
        with(URLClassLoader(patchBundles.map { it.toURI().toURL() }.toTypedArray())) {
            patchBundles.flatMap { patchBundle ->
                // Get the names of all classes in the DEX file.

                JarFile(patchBundle).entries().asSequence()
                    .filter { it.name.endsWith(".class") }
                    .map {
                        loadClass(it.name.replace('/', '.').replace(".class", ""))
                    }
            }
        }
    )

    /**
     * A [PatchBundleLoader] for [Dex] files.
     *
     * @param patchBundlesPath The path to or a path to a directory containing patch bundles of DEX format.
     */
    class Dex(private val patchBundlesPath: File) : PatchBundleLoader(
        with(PathClassLoader(patchBundlesPath.absolutePath, null)) {
            fun readDexFile(file: File) = MultiDexIO.readDexFile(
                true,
                file,
                BasicDexFileNamer(),
                null,
                null
            )

            // Get the names of all classes in the DEX file.

            val dexFiles = if (patchBundlesPath.isFile) listOf(readDexFile(patchBundlesPath))
            else patchBundlesPath.listFiles { it -> it.isFile }?.map { readDexFile(it) } ?: emptyList()

            dexFiles.flatMap { it.classes }.map { classDef ->
                classDef.type.substring(1, classDef.length - 1).replace('/', '.')
            }.map { loadClass(it) }
        }
    )
}