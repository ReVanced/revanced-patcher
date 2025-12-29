package app.revanced.patcher.patch

import kotlin.reflect.jvm.javaField
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

            this.apply {
                println(print)
                println(custom.value)
            }
        }

        assertEquals(2, patch.options.size)
    }

    @Test
    fun `loads patches correctly`() {
        val patchesClass = ::Public.javaField!!.declaringClass.name
        val classLoader = ::Public.javaClass.classLoader

        val patches = getPatches(listOf(patchesClass), classLoader)

        assertEquals(
            2,
            patches.size,
            "Expected 2 patches to be loaded, " +
                    "because there's only two named patches declared as public static fields " +
                    "or returned by public static and non-parametrized methods.",
        )
    }
}

val publicUnnamedPatch = bytecodePatch {} // Not loaded, because it's unnamed.

val Public by creatingBytecodePatch {} // Loaded, because it's named.

private val privateUnnamedPatch = bytecodePatch {} // Not loaded, because it's private.

private val Private by creatingBytecodePatch {}  // Not loaded, because it's private.

fun publicUnnamedPatchFunction() = publicUnnamedPatch // Not loaded, because it's unnamed.

fun publicNamedPatchFunction() = bytecodePatch("Public") { } // Loaded, because it's named.

fun parameterizedFunction(@Suppress("UNUSED_PARAMETER") param: Any) =
    publicNamedPatchFunction() // Not loaded, because it's parameterized.

private fun privateUnnamedPatchFunction() = privateUnnamedPatch // Not loaded, because it's private.

private fun privateNamedPatchFunction() = Private // Not loaded, because it's private.
