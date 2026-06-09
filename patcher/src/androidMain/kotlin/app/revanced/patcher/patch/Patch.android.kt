package app.revanced.patcher.patch

import com.android.tools.smali.dexlib2.dexbacked.DexBackedDexFile
import com.android.tools.smali.dexlib2.util.DexUtil
import dalvik.system.PathClassLoader
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
    { patchesFile ->
        val patchesFileBytes = patchesFile.readBytes()

        patchesFileBytes.also { DexUtil.verifyDexHeader(it, 0); }

        DexBackedDexFile(null, patchesFileBytes, 0).classes.map { classDef ->
            classDef.type.substring(1, classDef.length - 1)
        }
    },
    PathClassLoader(
        patchesFiles.joinToString(File.pathSeparator) { it.absolutePath },
        currentClassLoader,
    ),
    onFailedToLoad,
)

@Suppress("NOTHING_TO_INLINE")
actual inline val currentClassLoader get() = object {}::class.java.classLoader
