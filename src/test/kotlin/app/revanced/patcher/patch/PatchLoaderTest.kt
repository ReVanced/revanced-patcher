@file:Suppress("unused")

package app.revanced.patcher.patch

import org.junit.jupiter.api.Test
import kotlin.reflect.KFunction
import kotlin.reflect.full.*
import kotlin.reflect.jvm.javaField
import kotlin.reflect.jvm.javaMethod
import kotlin.test.assertEquals

// region Test patches.

val publicUnnamedPatch = bytecodePatch {
}
val publicPatch = bytecodePatch("Public") {
}
private val privateUnnamedPatch = bytecodePatch {
}
private val privatePatch = bytecodePatch("Private") {
}

fun publicUnnamedPatchFunction() = publicUnnamedPatch
fun publicNamedPatchFunction() = bytecodePatch("Public") { }
private fun privateUnnamedPatchFunction() = privateUnnamedPatch
private fun privateNamedPatchFunction() = privatePatch

// endregion

internal object PatchLoaderTest {
    private const val LOAD_PATCHES_FUNCTION_NAME = "loadPatches"
    private val TEST_PATCHES_CLASS = ::publicPatch.javaField!!.declaringClass.name
    private val TEST_PATCHES_CLASS_LOADER = ::publicPatch.javaClass.classLoader

    @Test
    fun `loads patches correctly`() {
        // Get instance of private PatchLoader.Companion class.
        val patchLoaderCompanionObject = PatchLoader::class.java.declaredFields.first {
            it.type == PatchLoader::class.companionObject!!.javaObjectType
        }.apply {
            isAccessible = true
        }.get(null)

        // Get private PatchLoader.Companion.loadPatches function from PatchLoader.Companion.
        @Suppress("UNCHECKED_CAST")
        val loadPatchesFunction = patchLoaderCompanionObject::class.declaredFunctions.first {
            it.name == LOAD_PATCHES_FUNCTION_NAME
        }.apply {
            javaMethod!!.isAccessible = true
        } as KFunction<Set<Patch<*>>>

        // Call private PatchLoader.Companion.loadPatches function.
        val patches = loadPatchesFunction.call(
            patchLoaderCompanionObject,
            TEST_PATCHES_CLASS_LOADER,
            listOf(TEST_PATCHES_CLASS),
        )

        assertEquals(
            2,
            patches.size,
            "Expected 2 patches to be loaded, " +
                "because there's only two named patches declared as a public static field or method.",
        )
    }
}
