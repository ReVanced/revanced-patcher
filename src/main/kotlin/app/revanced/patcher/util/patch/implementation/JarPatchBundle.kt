package app.revanced.patcher.util.patch.implementation

import app.revanced.patcher.util.patch.base.PatchBundle
import app.revanced.patcher.util.patch.util.StringIterator
import java.net.URLClassLoader
import java.util.jar.JarFile

/**
 * A patch bundle of the ReVanced [JarPatchBundle] format.
 * @param patchBundlePath The path to the patch bundle.
 */
class JarPatchBundle(patchBundlePath: String) : PatchBundle(patchBundlePath) {
    fun loadPatches() = loadPatches(URLClassLoader(arrayOf(this.toURI().toURL()), null), StringIterator(
        JarFile(this).entries().iterator()
    ) {
        it.realName.replace('/', '.').replace(".class", "")
    })
}
