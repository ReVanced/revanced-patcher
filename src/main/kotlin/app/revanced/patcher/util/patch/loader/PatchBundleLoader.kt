package app.revanced.patcher.util.patch.loader

import app.revanced.patcher.data.Data
import app.revanced.patcher.patch.Patch
import java.io.File

/**
 * @param path The path to the patch bundle.
 */
abstract class PatchBundleLoader(path: String) : File(path) {
    protected fun loadPatches(classLoader: ClassLoader, classNames: Iterator<String>) = buildList {
        for (className in classNames) {
            val clazz = classLoader.loadClass(className)
            if (!clazz.isAnnotationPresent(app.revanced.patcher.patch.annotations.Patch::class.java)) continue
            @Suppress("UNCHECKED_CAST") this.add(clazz as Class<out Patch<Data>>)
        }
    }
}