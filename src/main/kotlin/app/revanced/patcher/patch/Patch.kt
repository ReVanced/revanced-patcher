@file:Suppress("MemberVisibilityCanBePrivate")

package app.revanced.patcher.patch

import app.revanced.patcher.Patcher
import app.revanced.patcher.PatcherContext
import app.revanced.patcher.data.BytecodeContext
import app.revanced.patcher.data.Context
import app.revanced.patcher.data.ResourceContext
import app.revanced.patcher.fingerprint.MethodFingerprint
import app.revanced.patcher.patch.options.PatchOption
import app.revanced.patcher.patch.options.PatchOptions

typealias PackageName = String
typealias VersionName = String
typealias Package = Pair<PackageName, Set<VersionName>>

/**
 * A patch.
 *
 * @param C The [Context] type this patch will work on.
 * @property name The name of the patch.
 * @property description The description of the patch.
 * @property use Weather or not the patch should be used.
 * @property requiresIntegrations Weather or not the patch requires integrations.
 * @property compatibleWith The packages the patch is compatible with.
 * @property dependencies Other patches this patch depends on.
 * @property options The options of the patch.
 * @property executionBlock The execution block of the patch.
 * @property close The closing block of the patch. Called after all patches have been executed.
 *
 * @constructor Creates a new patch.
 */
sealed class Patch<C : Context<*>>(
    val name: String?,
    val description: String?,
    val use: Boolean,
    val requiresIntegrations: Boolean,
) {
    val compatiblePackages = mutableSetOf<Package>()
    val dependencies = mutableSetOf<Patch<*>>()
    val options = PatchOptions()

    private var executionBlock: ((C) -> Unit)? = null
    private var close: ((C) -> Unit)? = null

    /**
     * Adds compatible packages to the patch.
     *
     * @param block The block to build the compatible packages.
     */
    fun compatibleWith(block: CompatiblePackages.() -> Unit) {
        compatiblePackages.addAll(CompatiblePackages().apply(block).packages)
    }

    /**
     * Adds dependencies to the patch.
     *
     * @param patch The patches this patch depends on.
     */
    fun dependsOn(vararg patch: Patch<*>) {
        dependencies.addAll(patch)
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
     * Runs the execution block of the patch.
     */
    fun execute(context: C) {
        executionBlock?.invoke(context)
    }

    /**
     * Runs the execution block of the patch.
     * Called by [Patcher].
     *
     * @param context The [PatcherContext] to execute the patch on.
     */
    internal abstract fun execute(context: PatcherContext)

    /**
     * Sets the closing block of the patch.
     *
     * @param block The closing block of the patch.
     */
    fun close(block: C.() -> Unit) {
        close = block
    }

    /**
     * Runs the closing block of the patch.
     */
    fun close(context: C) {
        close?.invoke(context)
    }

    /**
     * Runs the closing block of the patch.
     */
    internal abstract fun close(context: PatcherContext)

    /**
     * Create a new [PatchOption] with a string value and add it to the current [Patch].
     *
     * @param key The key.
     * @param default The default value.
     * @param values Eligible patch option values mapped to a human-readable name.
     * @param title The title.
     * @param description A description.
     * @param required Whether the option is required.
     * @param validator The function to validate the option value.
     *
     * @return The created [PatchOption].
     *
     * @see PatchOption
     */
    fun stringPatchOption(
        key: String,
        default: String? = null,
        values: Map<String, String?>? = null,
        title: String? = null,
        description: String? = null,
        required: Boolean = false,
        validator: PatchOption<String>.(String?) -> Boolean = { true },
    ) = registerNewPatchOption(
        key,
        default,
        values,
        title,
        description,
        required,
        "String",
        validator,
    )

    /**
     * Create a new [PatchOption] with an integer value and add it to the current [Patch].
     *
     * @param key The key.
     * @param default The default value.
     * @param values Eligible patch option values mapped to a human-readable name.
     * @param title The title.
     * @param description A description.
     * @param required Whether the option is required.
     * @param validator The function to validate the option value.
     *
     * @return The created [PatchOption].
     *
     * @see PatchOption
     */
    fun intPatchOption(
        key: String,
        default: Int? = null,
        values: Map<String, Int?>? = null,
        title: String? = null,
        description: String? = null,
        required: Boolean = false,
        validator: PatchOption<Int?>.(Int?) -> Boolean = { true },
    ) = registerNewPatchOption(
        key,
        default,
        values,
        title,
        description,
        required,
        "Int",
        validator,
    )

    /**
     * Create a new [PatchOption] with a boolean value and add it to the current [Patch].
     *
     * @param key The key.
     * @param default The default value.
     * @param values Eligible patch option values mapped to a human-readable name.
     * @param title The title.
     * @param description A description.
     * @param required Whether the option is required.
     * @param validator The function to validate the option value.
     *
     * @return The created [PatchOption].
     *
     * @see PatchOption
     */
    fun booleanPatchOption(
        key: String,
        default: Boolean? = null,
        values: Map<String, Boolean?>? = null,
        title: String? = null,
        description: String? = null,
        required: Boolean = false,
        validator: PatchOption<Boolean?>.(Boolean?) -> Boolean = { true },
    ) = registerNewPatchOption(
        key,
        default,
        values,
        title,
        description,
        required,
        "Boolean",
        validator,
    )

    /**
     * Create a new [PatchOption] with a float value and add it to the current [Patch].
     *
     * @param key The key.
     * @param default The default value.
     * @param values Eligible patch option values mapped to a human-readable name.
     * @param title The title.
     * @param description A description.
     * @param required Whether the option is required.
     * @param validator The function to validate the option value.
     *
     * @return The created [PatchOption].
     *
     * @see PatchOption
     */
    fun floatPatchOption(
        key: String,
        default: Float? = null,
        values: Map<String, Float?>? = null,
        title: String? = null,
        description: String? = null,
        required: Boolean = false,
        validator: PatchOption<Float?>.(Float?) -> Boolean = { true },
    ) = registerNewPatchOption(
        key,
        default,
        values,
        title,
        description,
        required,
        "Float",
        validator,
    )

    /**
     * Create a new [PatchOption] with a long value and add it to the current [Patch].
     *
     * @param key The key.
     * @param default The default value.
     * @param values Eligible patch option values mapped to a human-readable name.
     * @param title The title.
     * @param description A description.
     * @param required Whether the option is required.
     * @param validator The function to validate the option value.
     *
     * @return The created [PatchOption].
     *
     * @see PatchOption
     */
    fun longPatchOption(
        key: String,
        default: Long? = null,
        values: Map<String, Long?>? = null,
        title: String? = null,
        description: String? = null,
        required: Boolean = false,
        validator: PatchOption<Long?>.(Long?) -> Boolean = { true },
    ) = registerNewPatchOption(
        key,
        default,
        values,
        title,
        description,
        required,
        "Long",
        validator,
    )

    /**
     * Create a new [PatchOption] with a string array value and add it to the current [Patch].
     *
     * @param key The key.
     * @param default The default value.
     * @param values Eligible patch option values mapped to a human-readable name.
     * @param title The title.
     * @param description A description.
     * @param required Whether the option is required.
     * @param validator The function to validate the option value.
     *
     * @return The created [PatchOption].
     *
     * @see PatchOption
     */
    fun stringArrayPatchOption(
        key: String,
        default: Array<String>? = null,
        values: Map<String, Array<String>?>? = null,
        title: String? = null,
        description: String? = null,
        required: Boolean = false,
        validator: PatchOption<Array<String>?>.(Array<String>?) -> Boolean = { true },
    ) = registerNewPatchOption(
        key,
        default,
        values,
        title,
        description,
        required,
        "StringArray",
        validator,
    )

    /**
     * Create a new [PatchOption] with an integer array value and add it to the current [Patch].
     *
     * @param key The key.
     * @param default The default value.
     * @param values Eligible patch option values mapped to a human-readable name.
     * @param title The title.
     * @param description A description.
     * @param required Whether the option is required.
     * @param validator The function to validate the option value.
     *
     * @return The created [PatchOption].
     *
     * @see PatchOption
     */
    fun intArrayPatchOption(
        key: String,
        default: Array<Int>? = null,
        values: Map<String, Array<Int>?>? = null,
        title: String? = null,
        description: String? = null,
        required: Boolean = false,
        validator: PatchOption<Array<Int>?>.(Array<Int>?) -> Boolean = { true },
    ) = registerNewPatchOption(
        key,
        default,
        values,
        title,
        description,
        required,
        "IntArray",
        validator,
    )

    /**
     * Create a new [PatchOption] with a boolean array value and add it to the current [Patch].
     *
     * @param key The key.
     * @param default The default value.
     * @param values Eligible patch option values mapped to a human-readable name.
     * @param title The title.
     * @param description A description.
     * @param required Whether the option is required.
     * @param validator The function to validate the option value.
     *
     * @return The created [PatchOption].
     *
     * @see PatchOption
     */
    fun booleanArrayPatchOption(
        key: String,
        default: Array<Boolean>? = null,
        values: Map<String, Array<Boolean>?>? = null,
        title: String? = null,
        description: String? = null,
        required: Boolean = false,
        validator: PatchOption<Array<Boolean>?>.(Array<Boolean>?) -> Boolean = { true },
    ) = registerNewPatchOption(
        key,
        default,
        values,
        title,
        description,
        required,
        "BooleanArray",
        validator,
    )

    /**
     * Create a new [PatchOption] with a float array value and add it to the current [Patch].
     *
     * @param key The key.
     * @param default The default value.
     * @param values Eligible patch option values mapped to a human-readable name.
     * @param title The title.
     * @param description A description.
     * @param required Whether the option is required.
     * @param validator The function to validate the option value.
     *
     * @return The created [PatchOption].
     *
     * @see PatchOption
     */
    fun floatArrayPatchOption(
        key: String,
        default: Array<Float>? = null,
        values: Map<String, Array<Float>?>? = null,
        title: String? = null,
        description: String? = null,
        required: Boolean = false,
        validator: PatchOption<Array<Float>?>.(Array<Float>?) -> Boolean = { true },
    ) = registerNewPatchOption(
        key,
        default,
        values,
        title,
        description,
        required,
        "FloatArray",
        validator,
    )

    /**
     * Create a new [PatchOption] with a long array value and add it to the current [Patch].
     *
     * @param key The key.
     * @param default The default value.
     * @param values Eligible patch option values mapped to a human-readable name.
     * @param title The title.
     * @param description A description.
     * @param required Whether the option is required.
     * @param validator The function to validate the option value.
     *
     * @return The created [PatchOption].
     *
     * @see PatchOption
     */
    fun longArrayPatchOption(
        key: String,
        default: Array<Long>? = null,
        values: Map<String, Array<Long>?>? = null,
        title: String? = null,
        description: String? = null,
        required: Boolean = false,
        validator: PatchOption<Array<Long>?>.(Array<Long>?) -> Boolean = { true },
    ) = registerNewPatchOption(
        key,
        default,
        values,
        title,
        description,
        required,
        "LongArray",
        validator,
    )

    /**
     * Create a new [PatchOption] and add it to the current [Patch].
     *
     * @param key The key.
     * @param default The default value.
     * @param values Eligible patch option values mapped to a human-readable name.
     * @param title The title.
     * @param description A description.
     * @param required Whether the option is required.
     * @param valueType The type of the option value (to handle type erasure).
     * @param validator The function to validate the option value.
     *
     * @return The created [PatchOption].
     *
     * @see PatchOption
     */
    fun <T> registerNewPatchOption(
        key: String,
        default: T? = null,
        values: Map<String, T?>? = null,
        title: String? = null,
        description: String? = null,
        required: Boolean = false,
        valueType: String,
        validator: PatchOption<T>.(T?) -> Boolean = { true },
    ) = PatchOption(
        key,
        default,
        values,
        title,
        description,
        required,
        valueType,
        validator,
    ).also(options::register)

    override fun toString() = name ?: "Patch"

    class CompatiblePackages {
        internal val packages = mutableSetOf<Package>()

        operator fun String.invoke(vararg versions: String) {
            packages.add(this to versions.toSet())
        }
    }
}

/**
 * A bytecode patch builder.
 *
 * @param name The name of the patch.
 * @param description The description of the patch.
 * @param use Weather or not the patch should be used.
 * @param requiresIntegrations Weather or not the patch requires integrations.
 *
 * @property fingerprints The fingerprints of the patch. Resolved before the patch is executed.
 *
 * @constructor Creates a new bytecode patch builder.
 */
class BytecodePatch internal constructor(
    name: String?,
    description: String?,
    use: Boolean,
    requiresIntegrations: Boolean,
) : Patch<BytecodeContext>(name, description, use, requiresIntegrations) {
    internal val fingerprints = mutableSetOf<MethodFingerprint>()

    /**
     * Adds fingerprints to the patch.
     *
     * @param fingerprint The fingerprints to add.
     */
    fun fingerprints(vararg fingerprint: MethodFingerprint) {
        fingerprints.addAll(fingerprint)
    }

    override fun execute(context: PatcherContext) = execute(context.bytecodeContext)
    override fun close(context: PatcherContext) = close(context.bytecodeContext)
}

/**
 * A raw resource patch builder.
 *
 * @param name The name of the patch.
 * @param description The description of the patch.
 * @param use Weather or not the patch should be used.
 * @param requiresIntegrations Weather or not the patch requires integrations.
 *
 * @constructor Creates a new raw resource patch builder.
 */
class RawResourcePatch internal constructor(
    name: String?,
    description: String?,
    use: Boolean,
    requiresIntegrations: Boolean,
) : Patch<ResourceContext>(name, description, use, requiresIntegrations) {
    override fun execute(context: PatcherContext) = execute(context.resourceContext)
    override fun close(context: PatcherContext) = close(context.resourceContext)
}

/**
 * A resource patch builder.
 *
 * @param name The name of the patch.
 * @param description The description of the patch.
 * @param use Weather or not the patch should be used.
 * @param requiresIntegrations Weather or not the patch requires integrations.
 *
 * @constructor Creates a new resource patch builder.
 */
class ResourcePatch internal constructor(
    name: String?,
    description: String?,
    use: Boolean,
    requiresIntegrations: Boolean,
) : Patch<ResourceContext>(name, description, use, requiresIntegrations) {
    override fun execute(context: PatcherContext) = execute(context.resourceContext)
    override fun close(context: PatcherContext) = close(context.resourceContext)
}

/**
 * Creates a new [BytecodePatch].
 *
 * @param name The name of the patch.
 * @param description The description of the patch.
 * @param use Weather or not the patch should be used.
 * @param requiresIntegrations Weather or not the patch requires integrations.
 * @param block The block to build the patch.
 * @return The created [BytecodePatch].
 */
fun bytecodePatch(
    name: String? = null,
    description: String? = null,
    use: Boolean = true,
    requiresIntegrations: Boolean = false,
    block: BytecodePatch.() -> Unit = {},
) = BytecodePatch(name, description, use, requiresIntegrations).apply(block)

/**
 * Creates a new [RawResourcePatch].
 *
 * @param name The name of the patch.
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
    block: RawResourcePatch.() -> Unit = {},
) = RawResourcePatch(name, description, use, requiresIntegrations).apply(block)

/**
 * Creates a new [ResourcePatch].
 *
 * @param name The name of the patch.
 * @param description The description of the patch.
 * @param use Weather or not the patch should be used.
 * @param requiresIntegrations Weather or not the patch requires integrations.
 * @param block The block to build the patch.
 * @return The created [ResourcePatch].
 */
fun resourcePatch(
    name: String? = null,
    description: String? = null,
    use: Boolean = true,
    requiresIntegrations: Boolean = false,
    block: ResourcePatch.() -> Unit = {},
) = ResourcePatch(name, description, use, requiresIntegrations).apply(block)
