@file:Suppress("unused")

package app.revanced.patcher.patch

import org.junit.jupiter.api.Test
import kotlin.reflect.jvm.javaField
import kotlin.test.assertEquals

internal object PatchLoaderTest {
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
