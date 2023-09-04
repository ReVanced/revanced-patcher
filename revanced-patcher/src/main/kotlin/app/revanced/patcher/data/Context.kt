package app.revanced.patcher.data

import java.util.function.Supplier

/**
 * A common interface for contexts such as [ResourceContext] and [BytecodeContext].
 */

sealed interface Context<T> : Supplier<T>