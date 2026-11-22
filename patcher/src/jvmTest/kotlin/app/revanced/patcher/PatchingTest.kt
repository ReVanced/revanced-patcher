package app.revanced.patcher

import app.revanced.patcher.patch.PatchException
import app.revanced.patcher.patch.bytecodePatch
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.TestInstance
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertSame

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class PatchingTest : PatcherTestBase() {
    @BeforeAll
    fun setup() = setupMock()

    @Test
    fun `reports patches that throw non-Exception throwables as failed`() {
        val linkError = NoSuchMethodError("'java.util.List.removeFirst()'")

        val errored = bytecodePatch("A Throws Error") { apply { throw linkError } }

        var dependentApplied = false
        val dependent =
            bytecodePatch("B Depends on Error") {
                dependsOn(errored)
                apply { dependentApplied = true }
            }

        val results = setOf(errored, dependent).applyAndCollectResults()

        assertFalse(dependentApplied, "A patch depending on a failed patch must not be applied.")

        val erroredResult = results.getValue(errored)
        assertIs<PatchException>(erroredResult.exception)
        assertSame(linkError, erroredResult.exception.cause)

        assertIs<PatchException>(results.getValue(dependent).exception)

        val failure = IllegalStateException("Failed")
        val exceptionPatch = bytecodePatch("Throws Exception") { apply { throw failure } }

        assertSame(failure, setOf(exceptionPatch).applyAndCollectResults().getValue(exceptionPatch).exception?.cause)
    }
}