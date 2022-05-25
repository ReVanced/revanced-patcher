package app.revanced.patcher.util.patch.base

import app.revanced.patcher.patch.base.Patch
import java.io.File

/**
 * @param patchBundlePath The path to the patch bundle.
 */
abstract class PatchBundle(patchBundlePath: String) : File(patchBundlePath) {
    internal fun loadPatches(classLoader: ClassLoader, classNames: Iterator<String>) = buildList {
        classNames.forEach { className ->
            val clazz = classLoader.loadClass(className)
            if (!clazz.isAnnotationPresent(app.revanced.patcher.patch.annotations.Patch::class.java)) return@forEach
            @Suppress("UNCHECKED_CAST") this.add(clazz as Class<Patch<*>>)
        }
    }
}