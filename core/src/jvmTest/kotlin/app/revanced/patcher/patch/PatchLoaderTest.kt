@file:Suppress("unused")

package app.revanced.patcher.patch

import io.mockk.mockk
import org.junit.jupiter.api.Test
import java.io.File
import kotlin.reflect.KFunction
import kotlin.reflect.full.companionObject
import kotlin.reflect.full.declaredFunctions
import kotlin.reflect.jvm.isAccessible
import kotlin.reflect.jvm.javaField
import kotlin.test.assertEquals

// region Test patches.

// Not loaded, because it's unnamed.
val publicUnnamedPatch = bytecodePatch {
}

// Loaded, because it's named.
val publicPatch = bytecodePatch("Public") {
}

// Not loaded, because it's private.
private val privateUnnamedPatch = bytecodePatch {
}

// Not loaded, because it's private.
private val privatePatch = bytecodePatch("Private") {
}

// Not loaded, because it's unnamed.
fun publicUnnamedPatchFunction() = publicUnnamedPatch

// Loaded, because it's named.
fun publicNamedPatchFunction() = bytecodePatch("Public") { }

// Not loaded, because it's parameterized.
fun parameterizedFunction(@Suppress("UNUSED_PARAMETER") param: Any) = publicNamedPatchFunction()

// Not loaded, because it's private.
private fun privateUnnamedPatchFunction() = privateUnnamedPatch

// Not loaded, because it's private.
private fun privateNamedPatchFunction() = privatePatch

// endregion

internal object PatchLoaderTest {
    private val TEST_PATCHES_CLASS = ::publicPatch.javaField!!.declaringClass.name
    private val TEST_PATCHES_CLASS_LOADER = ::publicPatch.javaClass.classLoader

    @Test
    fun `loads patches correctly`() {
        val patches = loadPatches(
            setOf(mockk<File>()),
            { listOf(TEST_PATCHES_CLASS) },
            TEST_PATCHES_CLASS_LOADER
        )

        assertEquals(
            2,
            patches.size,
            "Expected 2 patches to be loaded, " +
                    "because there's only two named patches declared as public static fields " +
                    "or returned by public static and non-parametrized methods.",
        )
    }
}
