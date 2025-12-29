package app.revanced.patcher

import app.revanced.patcher.patch.Patch
import app.revanced.patcher.patch.PatchException
import app.revanced.patcher.patch.bytecodePatch
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.api.assertThrows
import kotlin.test.Test
import kotlin.test.assertEquals

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class PatcherTest : PatcherTestBase() {
    @BeforeAll
    fun setup() = setupMock()

    @Test
    fun `applies patches in correct order`() {
        val applied = mutableListOf<String>()

        infix fun Patch.resultsIn(equals: List<String>) = this to equals
        infix fun Pair<Patch, List<String>>.because(reason: String) {
            runCatching { setOf(first)() }

            assertEquals(second, applied, reason)

            applied.clear()
        }

        bytecodePatch {
            dependsOn(
                bytecodePatch {
                    apply { applied += "1" }
                    afterDependents { applied += "-2" }
                },
                bytecodePatch { apply { applied += "2" } },
            )
            apply { applied += "3" }
            afterDependents { applied += "-1" }
        } resultsIn listOf("1", "2", "3", "-1", "-2") because
                "Patches should apply in post-order and afterDependents in pre-order."

        bytecodePatch {
            dependsOn(
                bytecodePatch {
                    apply { throw PatchException("1") }
                    afterDependents { applied += "-2" }
                },
            )
            apply { applied += "2" }
            afterDependents { applied += "-1" }
        } resultsIn emptyList() because
                "Patches that depend on a patched that failed to apply should not be applied."

        bytecodePatch {
            dependsOn(
                bytecodePatch {
                    apply { applied += "1" }
                    afterDependents { applied += "-2" }
                },
            )
            apply { throw PatchException("2") }
            afterDependents { applied += "-1" }
        } resultsIn listOf("1", "-2") because
                "afterDependents of a patch should not be called if it failed to apply."

        bytecodePatch {
            dependsOn(
                bytecodePatch {
                    apply { applied += "1" }
                    afterDependents { applied += "-2" }
                },
            )
            apply { applied += "2" }
            afterDependents { throw PatchException("-1") }
        } resultsIn listOf("1", "2", "-2") because
                "afterDependents of a patch should be called " +
                "regardless of dependant patches failing."
    }

    @Test
    fun `throws if unmatched fingerprint match is used`() {
        with(bytecodePatchContext) {
            val fingerprint = fingerprint { strings("doesnt exist") }

            assertThrows<PatchException>("Expected an exception because the fingerprint can't match.") {
                fingerprint.patternMatch
            }
        }
    }
}
