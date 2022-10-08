@file:Suppress("unused")

package app.revanced.patcher.util.patch

import app.revanced.patcher.data.Context
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
        for (className in classNames) {
            val clazz = classLoader.loadClass(className)
            if (!clazz.isAnnotationPresent(app.revanced.patcher.patch.annotations.Patch::class.java)) continue
            @Suppress("UNCHECKED_CAST") this.add(clazz as Class<out Patch<Context>>)
        }
    }

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
            StringIterator(
                JarFile(this)
                    .entries()
                    .toList() // TODO: find a cleaner solution than that to filter non class files
                    .filter {
                        it.name.endsWith(".class") && !it.name.contains("$")
                    }
                    .iterator()
            ) {
                it.realName.replace('/', '.').replace(".class", "")
            }
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
            StringIterator(DexFileFactory.loadDexFile(path, null).classes.iterator()) { classDef ->
                classDef.type.substring(1, classDef.length - 1).replace('/', '.')
            })
    }
}