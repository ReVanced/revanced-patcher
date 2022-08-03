package app.revanced.patcher.util.patch.impl

import app.revanced.patcher.util.patch.PatchBundle
import app.revanced.patcher.util.patch.StringIterator
import java.net.URLClassLoader
import java.util.jar.JarFile

/**
 * A patch bundle of the ReVanced [JarPatchBundle] format.
 * @param patchBundlePath The path to the patch bundle.
 */
class JarPatchBundle(patchBundlePath: String) : PatchBundle(patchBundlePath) {
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
