@file:Suppress("unused")

package app.revanced.patcher.util.patch

import app.revanced.patcher.extensions.AnnotationExtensions.findAnnotationRecursively
import app.revanced.patcher.patch.Patch
import app.revanced.patcher.patch.PatchClass
import dalvik.system.PathClassLoader
import org.jf.dexlib2.DexFileFactory
import java.io.File
import java.net.URLClassLoader
import java.util.jar.JarFile
import kotlin.streams.toList

/**
 * A patch bundle.
 *
 * @param fromClasses The classes to get [Patch]es from.
 */
sealed class PatchBundle private constructor(fromClasses: Iterable<Class<*>>) : Iterable<PatchClass> {
    private val patches = fromClasses.filter {
        if (it.isAnnotation) return@filter false

        it.findAnnotationRecursively(app.revanced.patcher.patch.annotations.Patch::class) != null
    }.map {
        @Suppress("UNCHECKED_CAST")
        it as PatchClass
    }

    override fun iterator() = patches.iterator()

    /**
     * A patch bundle of type [Jar].
     *
     * @param patchBundlePath The path to a patch bundle.
     */
    class Jar(private val patchBundlePath: File) : PatchBundle(
        with(URLClassLoader(arrayOf(patchBundlePath.toURI().toURL()), PatchBundle::class.java.classLoader)) {
            JarFile(patchBundlePath).stream().filter { it.name.endsWith(".class") }.map {
                loadClass(
                    it.realName.replace('/', '.').replace(".class", "")
                )
            }.toList()
        }
    )

    /**
     * A patch bundle of type [Dex] format.
     *
     * @param patchBundlePath The path to a patch bundle of dex format.
     */
    class Dex(private val patchBundlePath: File) : PatchBundle(
        with(PathClassLoader(patchBundlePath.absolutePath, null, PatchBundle::class.java.classLoader)) {
            DexFileFactory.loadDexFile(patchBundlePath, null).classes.map { classDef ->
                classDef.type.substring(1, classDef.length - 1).replace('/', '.')
            }.map { loadClass(it) }
        }
    )
}