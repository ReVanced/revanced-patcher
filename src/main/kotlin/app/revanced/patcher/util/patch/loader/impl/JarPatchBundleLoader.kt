package app.revanced.patcher.util.patch.loader.impl

import app.revanced.patcher.util.patch.loader.PatchBundleLoader
import java.net.URLClassLoader
import java.util.jar.JarFile

/**
 * A patch bundle of the ReVanced [JarPatchBundleLoader] format.
 * @param path The path to the patch bundle.
 */
class JarPatchBundleLoader(path: String) : PatchBundleLoader(path) {
    fun loadPatches() = loadPatches(
        URLClassLoader(
            arrayOf(this.toURI().toURL()),
            Thread.currentThread().contextClassLoader // TODO: find out why this is required
        ),
        app.revanced.patcher.util.patch.loader.StringIterator(
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
