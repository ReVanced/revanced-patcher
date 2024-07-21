package app.revanced.patcher.patch

import java.util.function.Supplier

/**
 * A common interface for contexts such as [ResourcePatchContext] and [BytecodePatchContext].
 */

sealed interface PatchContext<T> : Supplier<T>
