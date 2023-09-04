package app.revanced.patcher.patch.annotations.processor

import com.google.devtools.ksp.processing.SymbolProcessorEnvironment
import com.google.devtools.ksp.processing.SymbolProcessorProvider

class PatchProcessorProvider : SymbolProcessorProvider {
    override fun create(environment: SymbolProcessorEnvironment) =
        PatchProcessor(environment.codeGenerator, environment.logger)
}