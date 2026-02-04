@file:Suppress("MemberVisibilityCanBePrivate", "unused")

package app.revanced.patcher.patch

import java.io.File
import java.io.InputStream
import java.lang.reflect.Member
import java.lang.reflect.Method
import java.lang.reflect.Modifier
import java.util.function.Supplier
import kotlin.properties.ReadOnlyProperty

typealias PackageName = String
typealias VersionName = String
typealias Package = Pair<PackageName, Set<VersionName>?>

enum class PatchType(
    internal val prefix: String,
) {
    BYTECODE("Bytecode"),
    RAW_RESOURCE("RawResource"),
    RESOURCE("Resource"),
}

internal val Patch.patchesResources: Boolean get() = type == PatchType.RESOURCE || dependencies.any { it.patchesResources }

open class Patch internal constructor(
    val name: String?,
    val description: String?,
    val use: Boolean,
    val dependencies: Set<Patch>,
    val compatiblePackages: Set<Package>?,
    options: Set<Option<*>>,
    internal val apply: context(BytecodePatchContext, ResourcePatchContext) () -> Unit,
    // Must be nullable, so that Patcher.invoke can check,
    // if a patch has an "afterDependents" in order to not emit it twice.
    internal var afterDependents: (
    context(BytecodePatchContext, ResourcePatchContext)
        () -> Unit
    )?,
    internal val type: PatchType,
) {
    val options = Options(options)

    override fun toString() = name ?: "${type.prefix}Patch@${System.identityHashCode(this)}"
}

sealed class PatchBuilder<C : PatchContext<*>>(
    private val type: PatchType,
) {
    private var compatiblePackages: MutableSet<Package>? = null
    private val dependencies = mutableSetOf<Patch>()
    private val options = mutableSetOf<Option<*>>()

    internal var apply: context(BytecodePatchContext, ResourcePatchContext)
        () -> Unit = { }
    internal var afterDependents: (
    context(BytecodePatchContext, ResourcePatchContext)
        () -> Unit
    )? = null

    context(_: BytecodePatchContext, _: ResourcePatchContext)
    abstract val context: C

    open fun apply(block: C.() -> Unit) {
        apply = { block(context) }
    }

    fun afterDependents(block: C.() -> Unit) {
        afterDependents = { block(context) }
    }

    operator fun <T> Option<T>.invoke() = apply { options += this }

    operator fun String.invoke(vararg versions: VersionName) = invoke(versions.toSet())

    private operator fun String.invoke(versions: Set<VersionName>? = null): Package = this to versions

    fun compatibleWith(vararg packages: Package) {
        if (compatiblePackages == null) {
            compatiblePackages = mutableSetOf()
        }

        compatiblePackages!! += packages
    }

    fun compatibleWith(vararg packages: String) = compatibleWith(*packages.map { it() }.toTypedArray())

    fun dependsOn(vararg patches: Patch) {
        dependencies += patches
    }

    fun build(
        name: String?,
        description: String?,
        use: Boolean,
    ) = Patch(
        name,
        description,
        use,
        dependencies,
        compatiblePackages,
        options,
        apply,
        afterDependents,
        type,
    )
}

expect inline val currentClassLoader: ClassLoader

class BytecodePatchBuilder private constructor(
    @PublishedApi
    internal var getExtensionInputStream: (() -> InputStream)? = null,
) : PatchBuilder<BytecodePatchContext>(PatchType.BYTECODE) {
    internal constructor() : this(null)

    // Must be inline to access the patch's classloader.
    @Suppress("NOTHING_TO_INLINE")
    inline fun extendWith(extension: String) {
        // Should be the classloader which calls this function.
        val classLoader = currentClassLoader

        getExtensionInputStream = {
            classLoader.getResourceAsStream(extension)
                ?: throw PatchException("Extension \"$extension\" not found")
        }
    }

    fun extendWith(getExtensionStream: () -> InputStream) {
        getExtensionInputStream = getExtensionStream
    }

    context(_: BytecodePatchContext, _: ResourcePatchContext)
    override val context get() = contextOf<BytecodePatchContext>()

    init {
        apply = { context.addExtension() }
    }

    override fun apply(block: BytecodePatchContext.() -> Unit) {
        apply = {
            block(context.apply { addExtension() })
        }
    }

    private fun BytecodePatchContext.addExtension() = getExtensionInputStream?.let { get -> addExtension(get()) }
}

open class ResourcePatchBuilder internal constructor(
    type: PatchType,
) : PatchBuilder<ResourcePatchContext>(type) {
    internal constructor() : this(PatchType.RESOURCE)

    context(_: BytecodePatchContext, _: ResourcePatchContext)
    override val context get() = contextOf<ResourcePatchContext>()
}

class RawResourcePatchBuilder internal constructor() : ResourcePatchBuilder()

fun bytecodePatch(
    name: String? = null,
    description: String? = null,
    use: Boolean = true,
    block: BytecodePatchBuilder.() -> Unit,
) = BytecodePatchBuilder().apply(block).build(name, description, use)

fun resourcePatch(
    name: String? = null,
    description: String? = null,
    use: Boolean = true,
    block: ResourcePatchBuilder.() -> Unit,
) = ResourcePatchBuilder().apply(block).build(name, description, use)

fun rawResourcePatch(
    name: String? = null,
    description: String? = null,
    use: Boolean = true,
    block: RawResourcePatchBuilder.() -> Unit,
) = RawResourcePatchBuilder().apply(block).build(name, description, use)

private fun <B : PatchBuilder<*>> creatingPatch(
    description: String? = null,
    use: Boolean = true,
    block: B.() -> Unit,
    patchSupplier: (String?, String?, Boolean, B.() -> Unit) -> Patch,
) = ReadOnlyProperty<Any?, Patch> { _, property -> patchSupplier(property.name, description, use, block) }

fun creatingBytecodePatch(
    description: String? = null,
    use: Boolean = true,
    block: BytecodePatchBuilder.() -> Unit,
) = creatingPatch(description, use, block) { name, description, use, block ->
    bytecodePatch(name, description, use, block)
}

fun creatingResourcePatch(
    description: String? = null,
    use: Boolean = true,
    block: ResourcePatchBuilder.() -> Unit,
) = creatingPatch(description, use, block) { name, description, use, block ->
    resourcePatch(name, description, use, block)
}

fun creatingRawResourcePatch(
    description: String? = null,
    use: Boolean = true,
    block: RawResourcePatchBuilder.() -> Unit,
) = creatingPatch(description, use, block) { name, description, use, block ->
    rawResourcePatch(name, description, use, block)
}

/**
 * A common interface for contexts such as [ResourcePatchContext] and [BytecodePatchContext].
 */

sealed interface PatchContext<T> : Supplier<T>

/**
 * An exception thrown when patching.
 *
 * @param errorMessage The exception message.
 * @param cause The corresponding [Throwable].
 */
class PatchException(
    errorMessage: String?,
    cause: Throwable?,
) : Exception(errorMessage, cause) {
    constructor(errorMessage: String) : this(errorMessage, null)
    constructor(cause: Throwable) : this(cause.message, cause)
}

/**
 * A result of applying a [Patch].
 *
 * @param patch The [Patch] that ran.
 * @param exception The [PatchException] thrown, if any.
 */
class PatchResult internal constructor(
    val patch: Patch,
    val exception: PatchException? = null,
)

/**
 * Creates a [PatchResult] for this [Patch].
 *
 * @param exception The [PatchException] thrown, if any.
 * @return The created [PatchResult].
 */
internal fun Patch.patchResult(exception: Exception? = null) = PatchResult(this, exception?.toPatchException())

/**
 * Creates a [PatchResult] for this [Patch] with the given error message.
 *
 * @param errorMessage The error message.
 * @return The created [PatchResult].
 */
internal fun Patch.patchResult(errorMessage: String) = PatchResult(this, PatchException(errorMessage))

private fun Exception.toPatchException() = this as? PatchException ?: PatchException(this)

/**
 * A collection of patches loaded from patches files.
 *
 * @property patchesByFile The patches mapped by their patches file.
 */
class Patches internal constructor(
    val patchesByFile: Map<File, Set<Patch>>,
) : Set<Patch>
by patchesByFile.values.flatten().toSet()

// Must be internal and a separate function for testing.
@Suppress("MISSING_DEPENDENCY_IN_INFERRED_TYPE_ANNOTATION_WARNING")
internal fun getPatches(
    classNames: List<String>,
    classLoader: ClassLoader,
): Set<Patch> {
    fun Member.isUsable() =
        Modifier.isPublic(modifiers) && Modifier.isStatic(modifiers) && (this !is Method || parameterCount == 0)

    fun Class<*>.getPatchFields() =
        fields
            .filter { it.type.isPatch && it.isUsable() }
            .map { it.get(null) as Patch }

    fun Class<*>.getPatchMethods() =
        methods
            .filter { it.returnType.isPatch && it.parameterCount == 0 && it.isUsable() }
            .map { it.invoke(null) as Patch }

    return classNames
        .map { classLoader.loadClass(it) }
        .flatMap { it.getPatchMethods() + it.getPatchFields() }
        .filter { it.name != null }
        .toSet()
}

internal fun loadPatches(
    vararg patchesFiles: File,
    getBinaryClassNames: (patchesFile: File) -> List<String>,
    classLoader: ClassLoader,
    onFailedToLoad: (File, Throwable) -> Unit,
) = Patches(
    patchesFiles
        .map { file ->
            file to getBinaryClassNames(file)
        }.mapNotNull { (file, classNames) ->
            runCatching { file to getPatches(classNames, classLoader) }
                .onFailure { onFailedToLoad(file, it) }
                .getOrNull()
        }.toMap(),
)

expect fun loadPatches(
    vararg patchesFiles: File,
    onFailedToLoad: (patchesFile: File, throwable: Throwable) -> Unit = { _, _ -> },
): Patches

internal expect val Class<*>.isPatch: Boolean
