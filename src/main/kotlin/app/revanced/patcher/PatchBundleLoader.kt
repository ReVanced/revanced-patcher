@file:Suppress("unused")

package app.revanced.patcher

import app.revanced.patcher.extensions.AnnotationExtensions.findAnnotationRecursively
import app.revanced.patcher.extensions.PatchExtensions.patchName
import app.revanced.patcher.patch.Patch
import app.revanced.patcher.patch.PatchClass
import dalvik.system.DexClassLoader
import lanchon.multidexlib2.BasicDexFileNamer
import lanchon.multidexlib2.MultiDexIO
import java.io.File
import java.net.URLClassLoader
import java.util.jar.JarFile

/**
 * A patch bundle.
 *
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
        }.sortedBy {
            it.patchName
        }.let { addAll(it) }
    }

    /**
     * A [PatchBundleLoader] for JAR files.
     *
     * @param patchBundles The path to patch bundles of JAR format.
     */
    class Jar(vararg patchBundles: File) : PatchBundleLoader(
        with(
            URLClassLoader(patchBundles.map { it.toURI().toURL() }.toTypedArray())
        ) {
            patchBundles.flatMap { patchBundle ->
                // Get the names of all classes in the DEX file.

                JarFile(patchBundle).entries().asSequence()
                    .filter { it.name.endsWith(".class") }
                    .map { it.name.replace('/', '.').replace(".class", "") }
                    .map { loadClass(it) }
            }
        })

    /**
     * A [PatchBundleLoader] for [Dex] files.
     *
     * @param patchBundles The path to patch bundles of DEX format.
     * @param optimizedDexDirectory The directory to store optimized DEX files in.
     * This parameter is deprecated and has no effect since API level 26.
     */
    class Dex(vararg patchBundles: File, optimizedDexDirectory: File? = null) : PatchBundleLoader(
        with(
            DexClassLoader(
            patchBundles.joinToString(File.pathSeparator) { it.absolutePath }, optimizedDexDirectory?.absolutePath,
            null,
            PatchBundleLoader::class.java.classLoader
            )
        ) {
        patchBundles
            .flatMap {
                MultiDexIO.readDexFile(true, it, BasicDexFileNamer(), null, null).classes
            }
            .map { classDef -> classDef.type.substring(1, classDef.length - 1) }
            .map { loadClass(it) }
        }) {
        @Deprecated("This constructor is deprecated. Use the constructor with the second parameter instead.")
        constructor(vararg patchBundles: File) : this(*patchBundles, optimizedDexDirectory = null)
    }
}