package app.revanced.patcher.patch

import app.revanced.patcher.fingerprint.methodFingerprint
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
            "compatible.package"("1.0.0")
        }

        assertEquals(1, patch.compatiblePackages!!.size)
        assertEquals("compatible.package", patch.compatiblePackages!!.first().first)
    }

    @Test
    fun `can create patch with fingerprints`() {
        val externalFingerprint = methodFingerprint {}

        val patch = bytecodePatch(name = "Test") {
            val result by externalFingerprint()
            val internalFingerprint = methodFingerprint {}

            execute {
                result.method
                internalFingerprint.result
            }
        }

        assertEquals(2, patch.fingerprints.size)
    }

    @Test
    fun `can create patch with dependencies`() {
        val externalPatch = resourcePatch {}

        val patch = bytecodePatch(name = "Test") {
            externalPatch()
            resourcePatch {}
        }

        assertEquals(2, patch.dependencies.size)
        assertEquals(externalPatch, patch.dependencies.first())
    }

    @Test
    fun `can create patch with options`() {
        val patch = bytecodePatch(name = "Test") {
            val print by stringPatchOption("print")

            execute {
                println(print)
            }
        }

        assertEquals(1, patch.options.size)
    }
}
