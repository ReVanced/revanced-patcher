package app.revanced.patcher.util.patch

import app.revanced.patcher.patch.base.Patch
import java.io.File
import java.net.URLClassLoader
import java.util.jar.JarFile

object PatchLoader {
    /**
     * This method loads patches from a given jar file containing [Patch]es
     * @return the loaded patches represented as a list of [Patch] classes
     */
    fun loadFromFile(patchesJar: File) = buildList {
        val jarFile = JarFile(patchesJar)
        val classLoader = URLClassLoader(arrayOf(patchesJar.toURI().toURL()))

        val entries = jarFile.entries()
        while (entries.hasMoreElements()) {
            val entry = entries.nextElement()
            if (!entry.name.endsWith(".class") || entry.name.contains("$")) continue

            val clazz = classLoader.loadClass(entry.realName.replace('/', '.').replace(".class", ""))

            if (!clazz.isAnnotationPresent(app.revanced.patcher.patch.annotations.Patch::class.java)) continue

            @Suppress("UNCHECKED_CAST")
            val patch = clazz as Class<Patch<*>>

            // TODO: include declared classes from patch

            this.add(patch)
        }
    }
}