@file:Suppress("unused")

package app.revanced.patcher.util.patch

import app.revanced.patcher.data.Context
import app.revanced.patcher.extensions.AnnotationExtensions.findAnnotationRecursively
import app.revanced.patcher.extensions.PatchExtensions.patchName
import app.revanced.patcher.patch.Patch
import org.jf.dexlib2.DexFileFactory
import java.io.File
import java.net.URLClassLoader
import java.util.jar.JarFile

/**
 * A patch bundle.

 * @param path The path to the patch bundle.
 */
sealed class PatchBundle(path: String) : File(path) {
    internal fun loadPatches(classLoader: ClassLoader, classNames: Iterator<String>) = buildList {
        classNames.forEach { className ->
            val clazz = classLoader.loadClass(className)

            // Annotations can not Patch.
            if (clazz.isAnnotation) return@forEach

            clazz.findAnnotationRecursively(app.revanced.patcher.patch.annotations.Patch::class)
                ?: return@forEach

            @Suppress("UNCHECKED_CAST") this.add(clazz as Class<out Patch<Context>>)
        }
    }.sortedBy { it.patchName }

    /**
     * A patch bundle of type [Jar].
     *
     * @param patchBundlePath The path to the patch bundle.
     */
    class Jar(patchBundlePath: String) : PatchBundle(patchBundlePath) {

        /**
         * Load patches from the patch bundle.
         *
         * Patches will be loaded with a new [URLClassLoader].
         */
        fun loadPatches() = loadPatches(
            URLClassLoader(
                arrayOf(this.toURI().toURL()),
                Thread.currentThread().contextClassLoader // TODO: find out why this is required
            ),
            JarFile(this)
                .stream()
                .filter { it.name.endsWith(".class") && !it.name.contains("$") }
                .map { it.realName.replace('/', '.').replace(".class", "") }.iterator()
            )
    }

    /**
     * A patch bundle of type [Dex] format.
     *
     * @param patchBundlePath The path to a patch bundle of dex format.
     * @param dexClassLoader The dex class loader.
     */
    class Dex(patchBundlePath: String, private val dexClassLoader: ClassLoader) : PatchBundle(patchBundlePath) {
        /**
         * Load patches from the patch bundle.
         *
         * Patches will be loaded to the provided [dexClassLoader].
         */
        fun loadPatches() = loadPatches(dexClassLoader,
            DexFileFactory.loadDexFile(path, null).classes.asSequence().map { classDef ->
                classDef.type.substring(1, classDef.length - 1).replace('/', '.')
            }.iterator()
        )
    }
}