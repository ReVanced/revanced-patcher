package app.revanced.patcher

import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.TestInstance
import kotlin.test.Test
import kotlin.test.assertNotNull
import kotlin.test.assertNull

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class FingerprintTest : PatcherTestBase() {
    @BeforeAll
    fun setup() = setupMock()

    @Test
    fun `matches fingerprints correctly`() {
        with(bytecodePatchContext) {
            assertNotNull(
                fingerprint { returns("V") }.originalMethodOrNull,
                "Fingerprints should match correctly."
            )
            assertNull(
                fingerprint { returns("doesnt exist") }.originalMethodOrNull,
                "Fingerprints should match correctly."
            )
        }
    }
}
