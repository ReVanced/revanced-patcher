package app.revanced.patcher.patch.annotation.processor

import app.revanced.patcher.patch.Patch
import com.tschuchort.compiletesting.KotlinCompilation
import com.tschuchort.compiletesting.SourceFile
import com.tschuchort.compiletesting.kspWithCompilation
import com.tschuchort.compiletesting.symbolProcessorProviders
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class TestPatchAnnotationProcessor {
    // region Processing

    @Test
    fun testProcessing() = assertEquals(
        "Processable patch", compile(
            getSourceFile(
                "processing", "ProcessablePatch"
            )
        ).loadPatch("$SAMPLE_PACKAGE.processing.ProcessablePatchGenerated").name
    )

    // endregion

    // region Dependencies

    @Test
    fun testDependencies() {
        compile(
            getSourceFile(
                "dependencies", "DependentPatch"
            ), getSourceFile(
                "dependencies", "DependencyPatch"
            )
        ).let { result ->
            result.loadPatch("$SAMPLE_PACKAGE.dependencies.DependentPatchGenerated").let {
                // Dependency as well as the source class of the generated class.
                assertEquals(
                    2,
                    it.dependencies!!.size
                )

                // The last dependency is always the source class of the generated class to respect
                // order of dependencies.
                assertEquals(
                    result.loadPatch("$SAMPLE_PACKAGE.dependencies.DependentPatch")::class,
                    it.dependencies!!.last()
                )
            }
        }
    }

    // endregion

    // region Options

    @Test
    fun testOptions() {
        val patch = compile(
            getSourceFile(
                "options", "OptionsPatch"
            )
        ).loadPatch("$SAMPLE_PACKAGE.options.OptionsPatchGenerated")

        assert(patch.options.isNotEmpty())
        assertEquals(patch.options["print"].title, "Print message")
    }

    // endregion

    // region Limitations
    @Test
    fun failingManualDependency() = assertNull(
        compile(
            getSourceFile(
                "limitations/manualdependency", "DependentPatch"
            ), getSourceFile(
                "limitations/manualdependency", "DependencyPatch"
            )
        ).loadPatch("$SAMPLE_PACKAGE.limitations.manualdependency.DependentPatchGenerated").dependencies
    )

    // endregion

    private companion object Utils {
        const val SAMPLE_PACKAGE = "app.revanced.patcher.patch.annotation.processor.samples"

        /**
         * Get a source file from the given sample and class name.
         *
         * @param sample The sample to get the source file from.
         * @param className The name of the class to get the source file from.
         * @return The source file.
         */
        fun getSourceFile(sample: String, className: String): SourceFile {
            val resourceName = "app/revanced/patcher/patch/annotation/processor/samples/$sample/$className.kt"
            return SourceFile.kotlin(
                "$className.kt",
                TestPatchAnnotationProcessor::class.java.classLoader.getResourceAsStream(resourceName)
                    ?.readAllBytes()
                    ?.toString(Charsets.UTF_8)
                    ?: error("Could not find resource $resourceName")
            )
        }

        /**
         * Compile the given source files and return the result.
         *
         * @param sourceFiles The source files to compile.
         * @return The result of the compilation.
         */
        fun compile(vararg sourceFiles: SourceFile) = KotlinCompilation().apply {
            sources = sourceFiles.asList()

            symbolProcessorProviders = listOf(PatchProcessorProvider())

            // Required until https://github.com/tschuchortdev/kotlin-compile-testing/issues/312 closed.
            kspWithCompilation = true

            inheritClassPath = true
            messageOutputStream = System.out
        }.compile().also { result ->
            assertEquals(KotlinCompilation.ExitCode.OK, result.exitCode)
        }

        // region Class loading

        fun KotlinCompilation.Result.loadPatch(name: String) = classLoader.loadClass(name).loadPatch()

        fun Class<*>.loadPatch() = this.getField("INSTANCE").get(null) as Patch<*>

        // endregion
    }
}