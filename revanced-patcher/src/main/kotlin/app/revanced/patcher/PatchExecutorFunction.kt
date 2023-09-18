package app.revanced.patcher

import app.revanced.patcher.patch.PatchResult
import kotlinx.coroutines.flow.Flow
import java.util.function.Function

@FunctionalInterface
interface PatchExecutorFunction : Function<Boolean, Flow<PatchResult>>