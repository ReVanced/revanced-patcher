package app.revanced.patcher

import app.revanced.patcher.patch.Patch
import app.revanced.patcher.patch.PatchException
import app.revanced.patcher.patch.bytecodePatch
import app.revanced.patcher.patch.patchesResources
import app.revanced.patcher.patch.rawResourcePatch
import app.revanced.patcher.patch.resourcePatch
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.TestInstance
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class PatcherTest : PatcherTestBase() {
    @BeforeAll
    fun setup() = setupMock()

    @Test
    fun `detect resource patches`() {
        resourcePatch { }
            .patchesResources.let(::assertTrue)
        rawResourcePatch { }
            .patchesResources.let(::assertFalse)
        bytecodePatch { dependsOn(bytecodePatch { }, resourcePatch { }) }
            .patchesResources.let(::assertTrue)
        bytecodePatch { dependsOn(bytecodePatch { }, rawResourcePatch { }) }
            .patchesResources.let(::assertFalse)
    }

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
}
