package app.revanced.patcher.patch

import java.io.File
import java.net.URLClassLoader
import java.util.jar.JarFile

actual val Class<*>.isPatch get() = Patch::class.java.isAssignableFrom(this)

/**
 * Loads patches from JAR files declared as public static fields
 * or returned by public static and non-parametrized methods.
 * Patches with no name are not loaded. If a patches file fails to load,
 * the [onFailedToLoad] callback is invoked with the file and the throwable
 * and the loading continues for the other files.
 *
 * @param patchesFiles The JAR files to load the patches from.
 * @param onFailedToLoad A callback invoked when a patches file fails to load.
 *
 * @return The loaded patches.
 */
actual fun loadPatches(
    vararg patchesFiles: File,
    onFailedToLoad: (patchesFile: File, throwable: Throwable) -> Unit,
) = loadPatches(
    patchesFiles = patchesFiles,
    { file ->
        JarFile(file)
            .entries()
            .toList()
            .filter { it.name.endsWith(".class") }
            .map { it.name.substringBeforeLast('.').replace('/', '.') }
    },
    URLClassLoader(patchesFiles.map { it.toURI().toURL() }.toTypedArray()),
    onFailedToLoad = onFailedToLoad,
)

actual inline val currentClassLoader get() = object {}::class.java.classLoader
