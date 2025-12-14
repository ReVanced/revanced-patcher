package app.revanced.patcher.patch

import dalvik.system.DexClassLoader
import lanchon.multidexlib2.BasicDexFileNamer
import lanchon.multidexlib2.MultiDexIO
import java.io.File

actual val Class<*>.isPatch get() = Patch::class.java.isAssignableFrom(this)

/**
 * Loads patches from DEX files declared as public static fields
 * or returned by public static and non-parametrized methods.
 * Patches with no name are not loaded.
 *
 * @param patchesFiles The DEX files to load the patches from.
 *
 * @return The loaded patches.
 */
actual fun loadPatches(patchesFiles: Set<File>) = loadPatches(
    patchesFiles,
    { patchBundle ->
        MultiDexIO.readDexFile(true, patchBundle, BasicDexFileNamer(), null, null).classes
            .map { classDef ->
                classDef.type.substring(1, classDef.length - 1)
            }
    },
    DexClassLoader(
        patchesFiles.joinToString(File.pathSeparator) { it.absolutePath },
        null,
        null, null
    )
)
