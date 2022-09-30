@file:Suppress("unused")

package app.revanced.patcher.util.patch

import app.revanced.patcher.patch.Patch
import app.revanced.patcher.patch.PatchClass
import dalvik.system.DexClassLoader
import org.jf.dexlib2.DexFileFactory
import java.io.File
import java.net.URLClassLoader
import java.util.jar.JarFile

/**
 * A patch bundle.
 *
 * @param patchClassNames The class names of [Patch]es.
 */
abstract class PatchBundle(private val patchClassNames: Iterable<String>) {

    internal fun loadPatches(classLoader: ClassLoader) = buildList {
        patchClassNames.sorted().forEach { className ->
            classLoader.loadClass(className).let {
                if (!it.isAnnotationPresent(app.revanced.patcher.patch.annotations.Patch::class.java)) return@forEach
                @Suppress("UNCHECKED_CAST") this@buildList.add(it as PatchClass)
            }
        }
    }

    /**
     * A patch bundle of type [Jar].
     *
     * @param patchBundlePath The path to a patch bundle.
     */
    class Jar(patchBundlePath: String) : PatchBundle(JarFile(patchBundlePath).entries().toList()
        .filter { it.name.endsWith(".class") && !it.name.contains("$") }.map { classFile ->
            classFile.realName
                .let { it.substring(0, it.length - (CLASS_FILE_EXTENSION_LENGTH + 1)) } // remove file extension
                .replace('/', '.') // file path to package name
        }) {
        private val patchBundleUrls = arrayOf(File(patchBundlePath).toURI().toURL())

        /**
         * (Re)load patches from the patch bundle.
         **/
        fun loadPatches() = loadPatches(URLClassLoader(patchBundleUrls))

        companion object {
            private const val CLASS_FILE_EXTENSION_LENGTH = 5
        }
    }

    /**
     * A patch bundle of type [Dex] format.
     *
     * @param patchBundlePath The path to a patch bundle of dex format.
     */
    class Dex(private val patchBundlePath: String) : PatchBundle(
        DexFileFactory.loadDexFile(patchBundlePath, null).classes.map {
            it.type
                .substring(1, it.length - 1)
                .replace('/', '.')
        }
    ) {

        /**
         * (Re)load patches from the patch bundle.
         *
         * @param optimizedDirectory The directory for [DexClassLoader].
         */
        fun loadPatches(optimizedDirectory: String) = loadPatches(
            DexClassLoader(patchBundlePath, optimizedDirectory, null, null)
        )
    }
}