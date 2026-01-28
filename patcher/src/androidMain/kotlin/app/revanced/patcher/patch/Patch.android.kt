package app.revanced.patcher.patch

import dalvik.system.DexClassLoader
import lanchon.multidexlib2.BasicDexFileNamer
import lanchon.multidexlib2.MultiDexIO
import java.io.File

actual val Class<*>.isPatch get() = Patch::class.java.isAssignableFrom(this)

/**
 * Loads patches from DEX files declared as public static fields
 * or returned by public static and non-parametrized methods.
 * Patches with no name are not loaded. If a patches file fails to load,
 * the [onFailedToLoad] callback is invoked with the file and the throwable
 * and the loading continues for the other files.
 *
 * @param patchesFiles The DEX files to load the patches from.
 * @param onFailedToLoad A callback invoked when a patches file fails to load.
 *
 * @return The loaded patches.
 */
actual fun loadPatches(
    vararg patchesFiles: File,
    onFailedToLoad: (patchesFile: File, throwable: Throwable) -> Unit,
) = loadPatches(
    patchesFiles = patchesFiles,
    { patchBundle ->
        MultiDexIO
            .readDexFile(true, patchBundle, BasicDexFileNamer(), null, null)
            .classes
            .map { classDef ->
                classDef.type.substring(1, classDef.length - 1)
            }
    },
    DexClassLoader(
        patchesFiles.joinToString(File.pathSeparator) { it.absolutePath },
        null,
        null,
        null,
    ),
    onFailedToLoad,
)

@Suppress("NOTHING_TO_INLINE")
actual inline val currentClassLoader get() = object {}::class.java.classLoader
