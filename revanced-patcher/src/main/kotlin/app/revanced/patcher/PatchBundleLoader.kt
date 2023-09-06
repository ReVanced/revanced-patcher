@file:Suppress("unused")

package app.revanced.patcher

import app.revanced.patcher.patch.Patch
import dalvik.system.DexClassLoader
import lanchon.multidexlib2.BasicDexFileNamer
import lanchon.multidexlib2.MultiDexIO
import java.io.File
import java.net.URLClassLoader
import java.util.jar.JarFile
import java.util.logging.Logger
import kotlin.reflect.KClass

/**
 * A set of [Patch]es.
 */
typealias PatchSet = Set<Patch<*>>

/**
 * A [Patch] class.
 */
typealias PatchClass = KClass<out Patch<*>>

/**
 * A loader of [Patch]es from patch bundles.
 * This will load all [Patch]es from the given patch bundles that have a name.
 *
 * @param getBinaryClassNames A function that returns the binary names of all classes in a patch bundle.
 * @param classLoader The [ClassLoader] to use for loading the classes.
 */
sealed class PatchBundleLoader private constructor(
    classLoader: ClassLoader,
    patchBundles: Array<out File>,
    getBinaryClassNames: (patchBundle: File) -> List<String>,
) : PatchSet by mutableSetOf() {
    private val logger = Logger.getLogger(PatchBundleLoader::class.java.name)

    init {
        patchBundles.flatMap(getBinaryClassNames).asSequence().map {
            classLoader.loadClass(it)
        }.filter {
            it.isInstance(Patch::class.java)
        }.mapNotNull { patchClass ->
            patchClass.getInstance(logger)
        }.filter {
            it.name != null
        }.let { patches ->
            @Suppress("UNCHECKED_CAST")
            (this as MutableSet<Patch<*>>).addAll(patches)
        }
    }

    internal companion object Utils {
        /**
         * Instantiates a [Patch]. If the class is a singleton, the INSTANCE field will be used.
         *
         * @param logger The [Logger] to use for logging.
         * @return The instantiated [Patch] or `null` if the [Patch] could not be instantiated.
         */
        internal fun Class<*>.getInstance(logger: Logger): Patch<*>? {
            return try {
                getField("INSTANCE").get(null)
            } catch (exception: NoSuchFileException) {
                logger.fine(
                    "Patch class '${name}' has no INSTANCE field, therefor not a singleton. " +
                            "Will try to instantiate it."
                )

                try {
                    getDeclaredConstructor().newInstance()
                } catch (exception: Exception) {
                    logger.severe(
                        "Patch class '${name}' is not singleton and has no suitable constructor, " +
                                "therefor cannot be instantiated and will be ignored."
                    )

                    return null
                }
            } as Patch<*>
        }
    }

    /**
     * A [PatchBundleLoader] for JAR files.
     *
     * @param patchBundles The path to patch bundles of JAR format.
     */
    class Jar(vararg patchBundles: File) : PatchBundleLoader(
        URLClassLoader(patchBundles.map { it.toURI().toURL() }.toTypedArray()),
        patchBundles,
        { patchBundle ->
            JarFile(patchBundle).entries().toList().filter { it.name.endsWith(".class") }
                .map { it.name.replace('/', '.').replace(".class", "") }
        }
    )

    /**
     * A [PatchBundleLoader] for [Dex] files.
     *
     * @param patchBundles The path to patch bundles of DEX format.
     * @param optimizedDexDirectory The directory to store optimized DEX files in.
     * This parameter is deprecated and has no effect since API level 26.
     */
    class Dex(vararg patchBundles: File, optimizedDexDirectory: File? = null) : PatchBundleLoader(
        DexClassLoader(
            patchBundles.joinToString(File.pathSeparator) { it.absolutePath }, optimizedDexDirectory?.absolutePath,
            null,
            PatchBundleLoader::class.java.classLoader
        ),
        patchBundles,
        { patchBundle ->
            MultiDexIO.readDexFile(true, patchBundle, BasicDexFileNamer(), null, null).classes
                .map { classDef ->
                    classDef.type.substring(1, classDef.length - 1)
                }
        }
    ) {
        @Deprecated("This constructor is deprecated. Use the constructor with the second parameter instead.")
        constructor(vararg patchBundles: File) : this(*patchBundles, optimizedDexDirectory = null)
    }
}