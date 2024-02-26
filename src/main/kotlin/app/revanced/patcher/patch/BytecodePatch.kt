package app.revanced.patcher.patch

import app.revanced.patcher.PatchClass
import app.revanced.patcher.Patcher
import app.revanced.patcher.PatcherContext
import app.revanced.patcher.data.BytecodeContext
import app.revanced.patcher.fingerprint.MethodFingerprint
import app.revanced.patcher.fingerprint.MethodFingerprint.Companion.resolveUsingLookupMap
import java.io.Closeable

/**
 * A [Patch] that accesses a [BytecodeContext].
 *
 * If an implementation of [Patch] also implements [Closeable]
 * it will be closed in reverse execution order of patches executed by [Patcher].
 */
@Suppress("unused")
abstract class BytecodePatch : Patch<BytecodeContext> {
    /**
     * The fingerprints to resolve before executing the patch.
     */
    internal val fingerprints: Set<MethodFingerprint>

    /**
     * Create a new [BytecodePatch].
     *
     * @param fingerprints The fingerprints to resolve before executing the patch.
     */
    constructor(fingerprints: Set<MethodFingerprint> = emptySet()) {
        this.fingerprints = fingerprints
    }

    /**
     * Create a new [BytecodePatch].
     *
     * @param name The name of the patch.
     * @param description The description of the patch.
     * @param compatiblePackages The packages the patch is compatible with.
     * @param dependencies Other patches this patch depends on.
     * @param use Weather or not the patch should be used.
     * @param requiresIntegrations Weather or not the patch requires integrations.
     */
    constructor(
        name: String? = null,
        description: String? = null,
        compatiblePackages: Set<CompatiblePackage>? = null,
        dependencies: Set<PatchClass>? = null,
        use: Boolean = true,
        requiresIntegrations: Boolean = false,
        fingerprints: Set<MethodFingerprint> = emptySet(),
    ) : super(name, description, compatiblePackages, dependencies, use, requiresIntegrations) {
        this.fingerprints = fingerprints
    }

    /**
     * Create a new [BytecodePatch].
     */
    @Deprecated(
        "Use the constructor with fingerprints instead.",
        ReplaceWith("BytecodePatch(emptySet())"),
    )
    constructor() : this(emptySet())

    override fun execute(context: PatcherContext) {
        fingerprints.resolveUsingLookupMap(context.bytecodeContext)
        execute(context.bytecodeContext)
    }
}
