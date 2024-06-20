package app.revanced.patcher.patch

import app.revanced.patcher.fingerprint
import kotlin.test.Test
import kotlin.test.assertEquals

internal object PatchTest {
    @Test
    fun `can create patch with name`() {
        val patch = bytecodePatch(name = "Test") {}

        assertEquals("Test", patch.name)
    }

    @Test
    fun `can create patch with compatible packages`() {
        val patch = bytecodePatch(name = "Test") {
            compatibleWith(
                "compatible.package"("1.0.0"),
            )
        }

        assertEquals(1, patch.compatiblePackages!!.size)
        assertEquals("compatible.package", patch.compatiblePackages!!.first().first)
    }

    @Test
    fun `can create patch with fingerprints`() {
        val externalFingerprint = fingerprint {}

        val patch = bytecodePatch(name = "Test") {
            val externalFingerprintMatch by externalFingerprint()
            val internalFingerprintMatch by fingerprint {}

            execute {
                externalFingerprintMatch.method
                internalFingerprintMatch.method
            }
        }

        assertEquals(2, patch.fingerprints.size)
    }

    @Test
    fun `can create patch with dependencies`() {
        val patch = bytecodePatch(name = "Test") {
            dependsOn(resourcePatch {})
        }

        assertEquals(1, patch.dependencies.size)
    }

    @Test
    fun `can create patch with options`() {
        val patch = bytecodePatch(name = "Test") {
            val print by stringOption("print")
            val custom = option<String>("custom")()

            execute {
                println(print)
                println(custom.value)
            }
        }

        assertEquals(2, patch.options.size)
    }
}
