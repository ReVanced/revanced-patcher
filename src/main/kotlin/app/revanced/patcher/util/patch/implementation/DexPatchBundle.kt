package app.revanced.patcher.util.patch.implementation

import app.revanced.patcher.util.patch.base.PatchBundle
import app.revanced.patcher.util.patch.util.StringIterator
import org.jf.dexlib2.DexFileFactory

/**
 * A patch bundle of the ReVanced [DexPatchBundle] format.
 * @param patchBundlePath The path to a patch bundle of dex format.
 * @param dexClassLoader The dex class loader.
 */
class DexPatchBundle(patchBundlePath: String, private val dexClassLoader: ClassLoader) : PatchBundle(patchBundlePath) {
    fun loadPatches() = loadPatches(dexClassLoader,
        StringIterator(DexFileFactory.loadDexFile(path, null).classes.iterator()) { classDef ->
            classDef.type.substring(1, classDef.length - 1).replace('/', '.')
        })
}