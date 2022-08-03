package app.revanced.patcher.util.patch.loader.impl

import app.revanced.patcher.util.patch.loader.PatchBundleLoader
import org.jf.dexlib2.DexFileFactory

/**
 * A patch bundle of the ReVanced [DexPatchBundleLoader] format.
 * @param path The path to a patch bundle of dex format.
 * @param loader The dex class loader.
 */
class DexPatchBundleLoader(path: String, private val loader: ClassLoader) : PatchBundleLoader(path) {
    fun loadPatches() = loadPatches(loader,
        app.revanced.patcher.util.patch.loader.StringIterator(
            DexFileFactory.loadDexFile(
                path,
                null
            ).classes.iterator()
        ) { classDef ->
            classDef.type.substring(1, classDef.length - 1).replace('/', '.')
        })
}