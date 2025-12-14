package app.revanced.patcher.patch

import java.io.File
import java.net.URLClassLoader
import java.util.function.Predicate
import java.util.jar.JarFile

actual val Class<*>.isPatch get() = Patch::class.java.isAssignableFrom(this)

/**
 * Loads patches from JAR files declared as public static fields
 * or returned by public static and non-parametrized methods.
 * Patches with no name are not loaded.
 *
 * @param patchesFiles The JAR files to load the patches from.
 *
 * @return The loaded patches.
 */
actual fun loadPatches(patchesFiles: Set<File>) = loadPatches(
    patchesFiles,
    { file ->
        JarFile(file).entries().toList().filter { it.name.endsWith(".class") }
            .map { it.name.substringBeforeLast('.').replace('/', '.') }
    },
    URLClassLoader(patchesFiles.map { it.toURI().toURL() }.toTypedArray()),
)
