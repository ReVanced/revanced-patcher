@file:Suppress("MemberVisibilityCanBePrivate")

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
 * @constructor Creates a new patch.
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
     * @param context The [PatcherContext] to get the [PatchContext] from to close the patch with.
     */
    internal abstract fun finalize(context: PatcherContext)

    /**
     * Runs the finalizing block of the patch.
     *
     * @param context The [PatchContext] to close the patch with.
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
 * @constructor Creates a new bytecode patch.
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
 * @constructor Creates a new raw resource patch.
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
 * @constructor Creates a new resource patch.
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
 * @constructor Creates a new [Patch] builder.
 */
sealed class PatchBuilder<C : PatchContext<*>>(
    protected val name: String?,
    protected val description: String?,
    protected val use: Boolean,
    protected val requiresIntegrations: Boolean,
) {
    protected var compatiblePackages: MutableSet<Package>? = null
    protected val dependencies = mutableSetOf<Patch<*>>()
    protected val options = mutableSetOf<PatchOption<*>>()

    protected var executionBlock: ((C) -> Unit) = { }
    protected var finalizeBlock: ((C) -> Unit) = { }

    /**
     * Adds a compatible packages to the patch.
     *
     * @param versions The versions of the package.
     */
    operator fun String.invoke(vararg versions: String) {
        if (compatiblePackages == null) compatiblePackages = mutableSetOf()
        compatiblePackages!!.add(this to versions.toSet())
    }

    /**
     * Adds the patch as a dependency.
     *
     * @return The added patch.
     */
    operator fun Patch<*>.invoke() = apply {
        this@PatchBuilder.dependencies.add(this)
    }

    /**
     * Adds options to the patch.
     *
     * @param option The options to add.
     */
    fun option(vararg option: PatchOption<*>) {
        options.addAll(option)
    }

    /**
     * Sets the execution block of the patch.
     *
     * @param block The execution block of the patch.
     */
    fun execute(block: C.() -> Unit) {
        executionBlock = block
    }

    /**
     * Sets the finalizing block of the patch.
     *
     * @param block The closing block of the patch.
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
 * @constructor Creates a new [BytecodePatchBuilder] builder.
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
        ?: throw PatchException("Failed to resolve ${this.javaClass.simpleName}")

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
 * @constructor Creates a new [RawResourcePatch] builder.
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
 * @constructor Creates a new [ResourcePatch] builder.
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
 * Creates a new [BytecodePatch].
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
 * Creates a new [BytecodePatch] and adds it to the patch.
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
fun PatchBuilder<*>.bytecodePatch(
    name: String? = null,
    description: String? = null,
    use: Boolean = true,
    requiresIntegrations: Boolean = false,
    block: BytecodePatchBuilder.() -> Unit = {},
) = BytecodePatchBuilder(name, description, use, requiresIntegrations).buildPatch(block)() as BytecodePatch

/**
 * Creates a new [RawResourcePatch].
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
 * Creates a new [RawResourcePatch] and adds it to the patch.
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
fun PatchBuilder<*>.rawResourcePatch(
    name: String? = null,
    description: String? = null,
    use: Boolean = true,
    requiresIntegrations: Boolean = false,
    block: RawResourcePatchBuilder.() -> Unit = {},
) = RawResourcePatchBuilder(name, description, use, requiresIntegrations).buildPatch(block)() as RawResourcePatch

/**
 * Creates a new [ResourcePatch].
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
 * Creates a new [ResourcePatch] and adds it to the patch.
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
fun PatchBuilder<*>.resourcePatch(
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
 * Loads patches from JAR or DEX files declared as public fields or properties.
 * Patches with no name are not loaded.
 *
 * @param classLoader The [ClassLoader] to use for loading the classes.
 * @param getBinaryClassNames A function that returns the binary names of all classes accessible by the class loader.
 * @param patchesFiles A set of JAR or DEX files to load the patches from.
 */
sealed class PatchLoader private constructor(
    classLoader: ClassLoader,
    patchesFiles: Set<File>,
    getBinaryClassNames: (patchesFile: File) -> List<String>,
    // This constructor parameter is unfortunately necessary,
    // so that a reference to the mutable set is present in the constructor to be able to add patches to it.
    // because the instance itself is a PatchSet, which is immutable, that is delegated by the parameter.
    private val patchSet: MutableSet<Patch<*>> = mutableSetOf(),
) : PatchSet by patchSet {
    init {
        patchesFiles.asSequence().flatMap(getBinaryClassNames).map {
            classLoader.loadClass(it)
        }.flatMap {
            // Get all patches from the class declared as public fields.
            val patchFields = it.fields.filter { field ->
                Patch::class.java.isAssignableFrom(field.type)
            }.map { field -> field.get(null) as Patch<*> }

            // Get all patches from the class declared as public methods.
            val patchMethods = it.methods.filter { method ->
                Patch::class.java.isAssignableFrom(method.returnType)
            }.map { method -> method.invoke(null) as Patch<*> }

            patchFields + patchMethods
        }.filter {
            it.name != null
        }.toList().let { patches ->
            patchSet.addAll(patches)
        }
    }

    /**
     * A [PatchLoader] for JAR files.
     *
     * @param patchesFiles The JAR files to load the patches from.
     *
     * @constructor Creates a new [PatchLoader] for JAR files.
     */
    class Jar(vararg patchesFiles: File) : PatchLoader(
        URLClassLoader(patchesFiles.map { it.toURI().toURL() }.toTypedArray()),
        patchesFiles.toSet(),
        { file ->
            JarFile(file).entries().toList().filter { it.name.endsWith(".class") }
                .map { it.name.substringBeforeLast('.').replace('/', '.') }
        },
    )

    /**
     * A [PatchLoader] for [Dex] files.
     *
     * @param patchesFiles The DEX files to load the patches from.
     * @param optimizedDexDirectory The directory to store optimized DEX files in.
     * This parameter is deprecated and has no effect since API level 26.
     *
     * @constructor Creates a new [PatchLoader] for [Dex] files.
     */
    class Dex(vararg patchesFiles: File, optimizedDexDirectory: File? = null) : PatchLoader(
        DexClassLoader(
            patchesFiles.joinToString(File.pathSeparator) { it.absolutePath },
            optimizedDexDirectory?.absolutePath,
            null,
            PatchLoader::class.java.classLoader,
        ),
        patchesFiles.toSet(),
        { patchBundle ->
            MultiDexIO.readDexFile(true, patchBundle, BasicDexFileNamer(), null, null).classes
                .map { classDef ->
                    classDef.type.substring(1, classDef.length - 1)
                }
        },
    )
}
