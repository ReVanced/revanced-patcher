package app.revanced.patcher

import app.revanced.patcher.patch.Patch
import app.revanced.patcher.patch.PatchException
import app.revanced.patcher.patch.bytecodePatch
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.api.assertAll
import org.junit.jupiter.api.assertThrows
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class PatcherTest : PatcherTestBase() {
    @BeforeAll
    fun setUp() = setUpMock()

    @Test
    fun `executes patches in correct order`() {
        val executed = mutableListOf<String>()

        val patches = setOf(
            bytecodePatch { execute { executed += "1" } },
            bytecodePatch {
                dependsOn(
                    bytecodePatch {
                        execute { executed += "2" }
                        finalize { executed += "-2" }
                    },
                    bytecodePatch { execute { executed += "3" } },
                )

                execute { executed += "4" }
                finalize { executed += "-1" }
            },
        )

        assert(executed.isEmpty())

        patches()

        assertEquals(
            listOf("1", "2", "3", "4", "-1", "-2"),
            executed,
            "Expected patches to be executed in correct order.",
        )
    }

    @Test
    fun `handles execution of patches correctly when exceptions occur`() {
        val executed = mutableListOf<String>()

        infix fun Patch.resultsIn(equals: List<String>) {
            val patches = setOf(this)

            try {
                patches()
            } catch (_: PatchException) {
                // Swallow expected exceptions for testing purposes.
            }

            assertEquals(equals, executed, "Expected patches to be executed in correct order.")

            executed.clear()
        }

        // No patches execute successfully,
        // because the dependency patch throws an exception inside the execute block.
        bytecodePatch {
            dependsOn(
                bytecodePatch {
                    execute { throw PatchException("1") }
                    finalize { executed += "-2" }
                },
            )

            execute { executed += "2" }
            finalize { executed += "-1" }
        } resultsIn emptyList()

        // The dependency patch is executed successfully,
        // because only the dependant patch throws an exception inside the execute block.
        // Patches that depend on a failed patch should not be executed,
        // but patches that are dependant by a failed patch should be finalized.
        bytecodePatch {
            dependsOn(
                bytecodePatch {
                    execute { executed += "1" }
                    finalize { executed += "-2" }
                },
            )

            execute { throw PatchException("2") }
            finalize { executed += "-1" }
        } resultsIn listOf("1", "-2")

        // Because the finalize block of the dependency patch is executed after the finalize block of the dependant patch,
        // the dependant patch executes successfully, but the dependency patch raises an exception in the finalize block.
        bytecodePatch {
            dependsOn(
                bytecodePatch {
                    execute { executed += "1" }
                    finalize { throw PatchException("-2") }
                },
            )

            execute { executed += "2" }
            finalize { executed += "-1" }
        } resultsIn listOf("1", "2", "-1")

        // The dependency patch is executed successfully,
        // because the dependant patch raises an exception in the finalize block.
        // Patches that depend on a failed patch should not be executed,
        // but patches that are depended on by a failed patch should be executed.
        bytecodePatch {
            dependsOn(
                bytecodePatch {
                    execute { executed += "1" }
                    finalize { executed += "-2" }
                },
            )

            execute { executed += "2" }
            finalize { throw PatchException("-1") }
        } resultsIn listOf("1", "2", "-2")
    }

    @Test
    fun `throws if unmatched fingerprint match is used`() {
        with(bytecodePatchContext) {
            val fingerprint = fingerprint {
                strings("doesnt exist")
            }

            assertThrows<PatchException>("Expected an exception because the fingerprint can't match.") {
                fingerprint.patternMatch
            }
        }
    }


    @Test
    fun `matches fingerprint`() {
        val fingerprint = fingerprint { returns("V") }
        val fingerprint2 = fingerprint { returns("V") }
        val fingerprint3 = fingerprint { returns("V") }

        with(bytecodePatchContext) {
            assertAll(
                "Expected fingerprints to match.",
                { assertNotNull(fingerprint.matchOrNull(this.classDefs.first().methods.first())) },
                { assertNotNull(fingerprint2.matchOrNull(this.classDefs.first())) },
                { assertNotNull(fingerprint3.originalClassDefOrNull) },
            )
        }
    }
}
