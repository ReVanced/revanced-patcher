@file:Suppress("MemberVisibilityCanBePrivate", "unused")

package app.revanced.patcher.patch

import app.revanced.patcher.Patcher
import app.revanced.patcher.PatcherContext
import app.revanced.patcher.fingerprint.MethodFingerprint
import app.revanced.patcher.fingerprint.resolveUsingLookupMap
import dalvik.system.DexClassLoader
import lanchon.multidexlib2.BasicDexFileNamer
import lanchon.multidexlib2.MultiDexIO
import java.io.File
import java.net.URLClassLoader
import java.util.jar.JarFile
import kotlin.reflect.KProperty

/**
 * A set of [Patch].
 */
typealias PatchSet = Set<Patch<*>>

typealias PackageName = String
typealias VersionName = String
typealias Package = Pair<PackageName, Set<VersionName>>

/**
 * A patch.
 *
 * @param C The [PatchContext] to execute and finalize the patch with.
 * @property name The name of the patch.
 * If null, the patch is named "Patch" and will not be loaded by [PatchLoader].
 * @property description The description of the patch.
 * @property use Weather or not the patch should be used.
 * @property requiresIntegrations Weather or not the patch requires integrations.
 * @property compatiblePackages The packages the patch is compatible with.
 * If null, the patch is compatible with all packages.
 * @property dependencies Other patches this patch depends on.
 * @property options The options of the patch.
 * @property executeBlock The execution block of the patch.
 * @property finalizeBlock The finalizing block of the patch. Called after all patches have been executed,
 * in reverse order of execution.
 *
 * @constructor Create a new patch.
 */
sealed class Patch<C : PatchContext<*>>(
    val name: String?,
    val description: String?,
    val use: Boolean,
    val requiresIntegrations: Boolean,
    val compatiblePackages: Set<Package>?,
    val dependencies: Set<Patch<*>>,
    options: Set<PatchOption<*>>,
    private val executeBlock: ((C) -> Unit),
    private val finalizeBlock: ((C) -> Unit),
) {
    val options = PatchOptions(options)

    /**
     * Runs the execution block of the patch.
     * Called by [Patcher].
     *
     * @param context The [PatcherContext] to get the [PatchContext] from to execute the patch with.
     */
    internal abstract fun execute(context: PatcherContext)

    /**
     * Runs the execution block of the patch.
     *
     * @param context The [PatchContext] to execute the patch with.
     */
    fun execute(context: C) = executeBlock(context)

    /**
     * Runs the finalizing block of the patch.
     * Called by [Patcher].
     *
     * @param context The [PatcherContext] to get the [PatchContext] from to finalize the patch with.
     */
    internal abstract fun finalize(context: PatcherContext)

    /**
     * Runs the finalizing block of the patch.
     *
     * @param context The [PatchContext] to finalize the patch with.
     */
    fun finalize(context: C) = finalizeBlock(context)

    override fun toString() = name ?: "Patch"
}

/**
 * A bytecode patch.
 *
 * @param name The name of the patch.
 * If null, the patch is named "Patch" and will not be loaded by [PatchLoader].
 * @param description The description of the patch.
 * @param use Weather or not the patch should be used.
 * @param requiresIntegrations Weather or not the patch requires integrations.
 * @param compatiblePackages The packages the patch is compatible with.
 * If null, the patch is compatible with all packages.
 * @param dependencies Other patches this patch depends on.
 * @param options The options of the patch.
 * @param fingerprints The fingerprints that are resolved before the patch is executed.
 * @param executeBlock The execution block of the patch.
 * @param finalizeBlock The finalizing block of the patch. Called after all patches have been executed,
 * in reverse order of execution.
 *
 * @constructor Create a new bytecode patch.
 */
class BytecodePatch internal constructor(
    name: String?,
    description: String?,
    use: Boolean,
    requiresIntegrations: Boolean,
    compatiblePackages: Set<Package>?,
    dependencies: Set<Patch<*>>,
    options: Set<PatchOption<*>>,
    internal val fingerprints: Set<MethodFingerprint>,
    executeBlock: ((BytecodePatchContext) -> Unit),
    finalizeBlock: ((BytecodePatchContext) -> Unit),
) : Patch<BytecodePatchContext>(
    name,
    description,
    use,
    requiresIntegrations,
    compatiblePackages,
    dependencies,
    options,
    executeBlock,
    finalizeBlock,
) {
    override fun execute(context: PatcherContext) {
        fingerprints.resolveUsingLookupMap(context.bytecodeContext)

        execute(context.bytecodeContext)
    }

    override fun finalize(context: PatcherContext) = finalize(context.bytecodeContext)
}

/**
 * A raw resource patch.
 *
 * @param name The name of the patch.
 * If null, the patch is named "Patch" and will not be loaded by [PatchLoader].
 * @param description The description of the patch.
 * @param use Weather or not the patch should be used.
 * @param requiresIntegrations Weather or not the patch requires integrations.
 * @param compatiblePackages The packages the patch is compatible with.
 * If null, the patch is compatible with all packages.
 * @param dependencies Other patches this patch depends on.
 * @param options The options of the patch.
 * @param executeBlock The execution block of the patch.
 * @param finalizeBlock The finalizing block of the patch. Called after all patches have been executed,
 * in reverse order of execution.
 *
 * @constructor Create a new raw resource patch.
 */
class RawResourcePatch internal constructor(
    name: String?,
    description: String?,
    use: Boolean,
    requiresIntegrations: Boolean,
    compatiblePackages: Set<Package>?,
    dependencies: Set<Patch<*>>,
    options: Set<PatchOption<*>>,
    executeBlock: ((ResourcePatchContext) -> Unit),
    finalizeBlock: ((ResourcePatchContext) -> Unit),
) : Patch<ResourcePatchContext>(
    name,
    description,
    use,
    requiresIntegrations,
    compatiblePackages,
    dependencies,
    options,
    executeBlock,
    finalizeBlock,
) {
    override fun execute(context: PatcherContext) = execute(context.resourceContext)
    override fun finalize(context: PatcherContext) = finalize(context.resourceContext)
}

/**
 * A resource patch.
 *
 * @param name The name of the patch.
 * If null, the patch is named "Patch" and will not be loaded by [PatchLoader].
 * @param description The description of the patch.
 * @param use Weather or not the patch should be used.
 * @param requiresIntegrations Weather or not the patch requires integrations.
 * @param compatiblePackages The packages the patch is compatible with.
 * If null, the patch is compatible with all packages.
 * @param dependencies Other patches this patch depends on.
 * @param options The options of the patch.
 * @param executeBlock The execution block of the patch.
 * @param finalizeBlock The finalizing block of the patch. Called after all patches have been executed,
 * in reverse order of execution.
 *
 * @constructor Create a new resource patch.
 */
class ResourcePatch internal constructor(
    name: String?,
    description: String?,
    use: Boolean,
    requiresIntegrations: Boolean,
    compatiblePackages: Set<Package>?,
    dependencies: Set<Patch<*>>,
    options: Set<PatchOption<*>>,
    executeBlock: ((ResourcePatchContext) -> Unit),
    finalizeBlock: ((ResourcePatchContext) -> Unit),
) : Patch<ResourcePatchContext>(
    name,
    description,
    use,
    requiresIntegrations,
    compatiblePackages,
    dependencies,
    options,
    executeBlock,
    finalizeBlock,
) {
    override fun execute(context: PatcherContext) = execute(context.resourceContext)
    override fun finalize(context: PatcherContext) = finalize(context.resourceContext)
}

/**
 * A [Patch] builder.
 *
 * @param C The [PatchContext] to execute and finalize the patch with.
 * @property name The name of the patch.
 * If null, the patch is named "Patch" and will not be loaded by [PatchLoader].
 * @property description The description of the patch.
 * @property use Weather or not the patch should be used.
 * @property requiresIntegrations Weather or not the patch requires integrations.
 * @property compatiblePackages The packages the patch is compatible with.
 * If null, the patch is compatible with all packages.
 * @property dependencies Other patches this patch depends on.
 * @property option The options of the patch.
 * @property executionBlock The execution block of the patch.
 * @property finalizeBlock The finalizing block of the patch. Called after all patches have been executed,
 * in reverse order of execution.
 *
 * @constructor Create a new [Patch] builder.
 */
sealed class PatchBuilder<C : PatchContext<*>>(
    protected val name: String?,
    protected val description: String?,
    protected val use: Boolean,
    protected val requiresIntegrations: Boolean,
) {
    protected var compatiblePackages: Set<Package>? = null
    protected var dependencies: Set<Patch<*>> = emptySet()
    protected val options = mutableSetOf<PatchOption<*>>()

    protected var executionBlock: ((C) -> Unit) = { }
    protected var finalizeBlock: ((C) -> Unit) = { }

    /**
     * Sets the compatible packages of the patch.
     *
     * @param block The block to set the compatible packages with.
     */
    fun compatibleWith(block: CompatibleWithBuilder.() -> Unit) {
        compatiblePackages = CompatibleWithBuilder().apply(block).build()
    }

    /**
     * Add options to the patch.
     *
     * @param option The options to add.
     */
    fun option(vararg option: PatchOption<*>) {
        options.addAll(option)
    }

    /**
     * Add dependencies to the patch.
     *
     * @param block The block to add the dependencies with.
     */
    fun dependsOn(block: DependenciesBuilder.() -> Unit) {
        dependencies = DependenciesBuilder().apply(block).build()
    }

    /**
     * Set the execution block of the patch.
     *
     * @param block The execution block of the patch.
     */
    fun execute(block: C.() -> Unit) {
        executionBlock = block
    }

    /**
     * Set the finalizing block of the patch.
     *
     * @param block The finalizing block of the patch.
     */
    fun finalize(block: C.() -> Unit) {
        finalizeBlock = block
    }

    /**
     * Builds the patch.
     *
     * @return The built patch.
     */
    internal abstract fun build(): Patch<C>

    /**
     * A builder for dependencies.
     *
     * @constructor Create a new [DependenciesBuilder].
     */
    class DependenciesBuilder internal constructor() {
        private val dependencies = mutableSetOf<Patch<*>>()

        /**
         * Add the patch as a dependency.
         *
         * @return The added patch.
         */
        operator fun Patch<*>.invoke() = apply {
            this@DependenciesBuilder.dependencies.add(this)
        }

        internal fun build(): Set<Patch<*>> = dependencies
    }

    /**
     * A builder for compatible packages.
     *
     * @constructor Create a new [CompatibleWithBuilder].
     */
    class CompatibleWithBuilder internal constructor() {
        private val compatiblePackages = mutableSetOf<Package>()

        /**
         * Add a compatible package to the patch.
         *
         * @param versions The versions of the package.
         */
        operator fun String.invoke(vararg versions: String) {
            compatiblePackages += this to versions.toSet()
        }

        internal fun build(): Set<Package> = compatiblePackages
    }
}

/**
 * A [BytecodePatchBuilder] builder.
 *
 * @param name The name of the patch.
 * If null, the patch is named "Patch" and will not be loaded by [PatchLoader].
 * @param description The description of the patch.
 * @param use Weather or not the patch should be used.
 * @param requiresIntegrations Weather or not the patch requires integrations.
 *
 * @property fingerprints The fingerprints that are resolved before the patch is executed.
 *
 * @constructor Create a new [BytecodePatchBuilder] builder.
 */
class BytecodePatchBuilder internal constructor(
    name: String?,
    description: String?,
    use: Boolean,
    requiresIntegrations: Boolean,
) : PatchBuilder<BytecodePatchContext>(name, description, use, requiresIntegrations) {
    internal val fingerprints = mutableSetOf<MethodFingerprint>()

    /**
     * Add the fingerprint to the patch.
     */
    operator fun MethodFingerprint.invoke() = apply {
        fingerprints.add(MethodFingerprint(returnType, accessFlags, parameters, opcodes, strings, custom))
    }

    operator fun MethodFingerprint.getValue(nothing: Nothing?, property: KProperty<*>) = result
        ?: throw PatchException("Can't delegate unresolved fingerprint result to ${property.name}.")

    override fun build() = BytecodePatch(
        name,
        description,
        use,
        requiresIntegrations,
        compatiblePackages,
        dependencies,
        options,
        fingerprints,
        executionBlock,
        finalizeBlock,
    )
}

/**
 * A [RawResourcePatch] builder.
 *
 * @param name The name of the patch.
 * If null, the patch is named "Patch" and will not be loaded by [PatchLoader].
 * @param description The description of the patch.
 * @param use Weather or not the patch should be used.
 * @param requiresIntegrations Weather or not the patch requires integrations.
 *
 * @constructor Create a new [RawResourcePatch] builder.
 */
class RawResourcePatchBuilder internal constructor(
    name: String?,
    description: String?,
    use: Boolean,
    requiresIntegrations: Boolean,
) : PatchBuilder<ResourcePatchContext>(name, description, use, requiresIntegrations) {
    override fun build() = RawResourcePatch(
        name,
        description,
        use,
        requiresIntegrations,
        compatiblePackages,
        dependencies,
        options,
        executionBlock,
        finalizeBlock,
    )
}

/**
 * A [ResourcePatch] builder.
 *
 * @param name The name of the patch.
 * If null, the patch is named "Patch" and will not be loaded by [PatchLoader].
 * @param description The description of the patch.
 * @param use Weather or not the patch should be used.
 * @param requiresIntegrations Weather or not the patch requires integrations.
 *
 * @constructor Create a new [ResourcePatch] builder.
 */
class ResourcePatchBuilder internal constructor(
    name: String?,
    description: String?,
    use: Boolean,
    requiresIntegrations: Boolean,
) : PatchBuilder<ResourcePatchContext>(name, description, use, requiresIntegrations) {
    override fun build() = ResourcePatch(
        name,
        description,
        use,
        requiresIntegrations,
        compatiblePackages,
        dependencies,
        options,
        executionBlock,
        finalizeBlock,
    )
}

/**
 * Builds a [Patch].
 *
 * @param B The [PatchBuilder] to build the patch with.
 * @param block The block to build the patch.
 *
 * @return The built [Patch].
 */
private fun <B : PatchBuilder<*>> B.buildPatch(block: B.() -> Unit = {}) = apply(block).build()

/**
 * Create a new [BytecodePatch].
 *
 * @param name The name of the patch.
 * If null, the patch is named "Patch" and will not be loaded by [PatchLoader].
 * @param description The description of the patch.
 * @param use Weather or not the patch should be used.
 * @param requiresIntegrations Weather or not the patch requires integrations.
 * @param block The block to build the patch.
 *
 * @return The created [BytecodePatch].
 */
fun bytecodePatch(
    name: String? = null,
    description: String? = null,
    use: Boolean = true,
    requiresIntegrations: Boolean = false,
    block: BytecodePatchBuilder.() -> Unit = {},
) = BytecodePatchBuilder(name, description, use, requiresIntegrations).buildPatch(block) as BytecodePatch

/**
 * Create a new [BytecodePatch] and add it to the patch.
 *
 * @param name The name of the patch.
 * If null, the patch is named "Patch" and will not be loaded by [PatchLoader].
 * @param description The description of the patch.
 * @param use Weather or not the patch should be used.
 * @param requiresIntegrations Weather or not the patch requires integrations.
 * @param block The block to build the patch.
 *
 * @return The created [BytecodePatch].
 */
fun PatchBuilder.DependenciesBuilder.bytecodePatch(
    name: String? = null,
    description: String? = null,
    use: Boolean = true,
    requiresIntegrations: Boolean = false,
    block: BytecodePatchBuilder.() -> Unit = {},
) = BytecodePatchBuilder(name, description, use, requiresIntegrations).buildPatch(block)() as BytecodePatch

/**
 * Create a new [RawResourcePatch].
 *
 * @param name The name of the patch.
 * If null, the patch is named "Patch" and will not be loaded by [PatchLoader].
 * @param description The description of the patch.
 * @param use Weather or not the patch should be used.
 * @param requiresIntegrations Weather or not the patch requires integrations.
 * @param block The block to build the patch.
 * @return The created [RawResourcePatch].
 */
fun rawResourcePatch(
    name: String? = null,
    description: String? = null,
    use: Boolean = true,
    requiresIntegrations: Boolean = false,
    block: RawResourcePatchBuilder.() -> Unit = {},
) = RawResourcePatchBuilder(name, description, use, requiresIntegrations).buildPatch(block) as RawResourcePatch

/**
 * Create a new [RawResourcePatch] and add it to the patch.
 *
 * @param name The name of the patch.
 * If null, the patch is named "Patch" and will not be loaded by [PatchLoader].
 * @param description The description of the patch.
 * @param use Weather or not the patch should be used.
 * @param requiresIntegrations Weather or not the patch requires integrations.
 * @param block The block to build the patch.
 *
 * @return The created [RawResourcePatch].
 */
fun PatchBuilder.DependenciesBuilder.rawResourcePatch(
    name: String? = null,
    description: String? = null,
    use: Boolean = true,
    requiresIntegrations: Boolean = false,
    block: RawResourcePatchBuilder.() -> Unit = {},
) = RawResourcePatchBuilder(name, description, use, requiresIntegrations).buildPatch(block)() as RawResourcePatch

/**
 * Create a new [ResourcePatch].
 *
 * @param name The name of the patch.
 * If null, the patch is named "Patch" and will not be loaded by [PatchLoader].
 * @param description The description of the patch.
 * @param use Weather or not the patch should be used.
 * @param requiresIntegrations Weather or not the patch requires integrations.
 * @param block The block to build the patch.
 *
 * @return The created [ResourcePatch].
 */
fun resourcePatch(
    name: String? = null,
    description: String? = null,
    use: Boolean = true,
    requiresIntegrations: Boolean = false,
    block: ResourcePatchBuilder.() -> Unit = {},
) = ResourcePatchBuilder(name, description, use, requiresIntegrations).buildPatch(block) as ResourcePatch

/**
 * Create a new [ResourcePatch] and add it to the patch.
 *
 * @param name The name of the patch.
 * If null, the patch is named "Patch" and will not be loaded by [PatchLoader].
 * @param description The description of the patch.
 * @param use Weather or not the patch should be used.
 * @param requiresIntegrations Weather or not the patch requires integrations.
 * @param block The block to build the patch.
 *
 * @return The created [ResourcePatch].
 */
fun PatchBuilder.DependenciesBuilder.resourcePatch(
    name: String? = null,
    description: String? = null,
    use: Boolean = true,
    requiresIntegrations: Boolean = false,
    block: ResourcePatchBuilder.() -> Unit = {},
) = ResourcePatchBuilder(name, description, use, requiresIntegrations).buildPatch(block)() as ResourcePatch

/**
 * An exception thrown when patching.
 *
 * @param errorMessage The exception message.
 * @param cause The corresponding [Throwable].
 */
class PatchException(errorMessage: String?, cause: Throwable?) : Exception(errorMessage, cause) {
    constructor(errorMessage: String) : this(errorMessage, null)
    constructor(cause: Throwable) : this(cause.message, cause)
}

/**
 * A result of executing a [Patch].
 *
 * @param patch The [Patch] that was executed.
 * @param exception The [PatchException] thrown, if any.
 */
class PatchResult internal constructor(val patch: Patch<*>, val exception: PatchException? = null)

/**
 * A loader for [Patch].
 * Loads patches from JAR or DEX files declared as public static fields
 * or returned by public static and non-parametrized methods.
 * Patches with no name are not loaded.
 *
 * @param patchesFiles A set of JAR or DEX files to load the patches from.
 * @param getBinaryClassNames A function that returns the binary names of all classes accessible by the class loader.
 * @param classLoader The [ClassLoader] to use for loading the classes.
 */
sealed class PatchLoader private constructor(
    patchesFiles: Set<File>,
    private val getBinaryClassNames: (patchesFile: File) -> List<String>,
    private val classLoader: ClassLoader,
) : PatchSet by classLoader.loadPatches(patchesFiles.flatMap(getBinaryClassNames)) {
    /**
     * A [PatchLoader] for JAR files.
     *
     * @param patchesFiles The JAR files to load the patches from.
     *
     * @constructor Create a new [PatchLoader] for JAR files.
     */
    internal class Jar(patchesFiles: Set<File>) : PatchLoader(
        patchesFiles,
        { file ->
            JarFile(file).entries().toList().filter { it.name.endsWith(".class") }
                .map { it.name.substringBeforeLast('.').replace('/', '.') }
        },
        URLClassLoader(patchesFiles.map { it.toURI().toURL() }.toTypedArray()),
    )

    /**
     * A [PatchLoader] for [Dex] files.
     *
     * @param patchesFiles The DEX files to load the patches from.
     * @param optimizedDexDirectory The directory to store optimized DEX files in.
     * This parameter is deprecated and has no effect since API level 26.
     *
     * @constructor Create a new [PatchLoader] for [Dex] files.
     */
    internal class Dex(patchesFiles: Set<File>, optimizedDexDirectory: File? = null) : PatchLoader(
        patchesFiles,
        { patchBundle ->
            MultiDexIO.readDexFile(true, patchBundle, BasicDexFileNamer(), null, null).classes
                .map { classDef ->
                    classDef.type.substring(1, classDef.length - 1)
                }
        },
        DexClassLoader(
            patchesFiles.joinToString(File.pathSeparator) { it.absolutePath },
            optimizedDexDirectory?.absolutePath,
            null,
            PatchLoader::class.java.classLoader,
        ),
    )

    // Companion object required for unit tests.
    private companion object {
        /**
         * Loads named patches declared as public static fields
         * or returned by public static and non-parametrized methods.
         *
         * @param binaryClassNames The binary class name of the classes to load the patches from.
         *
         * @return The loaded patches.
         */
        private fun ClassLoader.loadPatches(binaryClassNames: List<String>) = binaryClassNames.asSequence().map {
            loadClass(it)
        }.flatMap {
            val isPatch = { cls: Class<*> -> Patch::class.java.isAssignableFrom(cls) }

            val patchesFromFields = it.fields.filter { field ->
                isPatch(field.type) && field.canAccess(null)
            }.map { field ->
                field.get(null) as Patch<*>
            }

            val patchesFromMethods = it.methods.filter { method ->
                isPatch(method.returnType) && method.parameterCount == 0 && method.canAccess(null)
            }.map { method ->
                method.invoke(null) as Patch<*>
            }

            patchesFromFields + patchesFromMethods
        }.filter {
            it.name != null
        }.toSet()
    }
}

/**
 * Loads patches from JAR files declared as public static fields
 * or returned by public static and non-parametrized methods.
 * Patches with no name are not loaded.
 *
 * @param patchesFiles The JAR files to load the patches from.
 *
 * @return The loaded patches.
 */
fun loadPatchesFromJar(patchesFiles: Set<File>): PatchSet =
    PatchLoader.Jar(patchesFiles)

/**
 * Loads patches from DEX files declared as public static fields
 * or returned by public static and non-parametrized methods.
 * Patches with no name are not loaded.
 *
 * @param patchesFiles The DEX files to load the patches from.
 *
 * @return The loaded patches.
 */
fun loadPatchesFromDex(patchesFiles: Set<File>, optimizedDexDirectory: File? = null): PatchSet =
    PatchLoader.Dex(patchesFiles, optimizedDexDirectory)
