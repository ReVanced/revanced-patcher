package app.revanced.patcher

import org.junit.jupiter.api.Test

internal class PatcherTest {
    @Test
    fun testPatcher() {
        return // FIXME: create a proper resource to pass this test
        /**
        val patcher = Patcher(
        File(PatcherTest::class.java.getResource("/example.apk")!!.toURI()),
        "exampleCacheDirectory",
        patchResources = true
        )

        patcher.addPatches(listOf(ExampleBytecodePatch(), ExampleResourcePatch()))

        for (signature in patcher.resolveSignatures()) {
        if (!signature.resolved) {
        throw Exception("Signature ${signature.metadata.name} was not resolved!")
        }
        val patternScanMethod = signature.metadata.patternScanMethod
        if (patternScanMethod is PatternScanMethod.Fuzzy) {
        val warnings = patternScanMethod.warnings
        if (warnings != null) {
        println("Signature ${signature.metadata.name} had ${warnings.size} warnings!")
        for (warning in warnings) {
        println(warning.toString())
        }
        } else {
        println("Signature ${signature.metadata.name} used the fuzzy resolver, but the warnings list is null!")
        }
        }
        }
        for ((metadata, result) in patcher.applyPatches()) {
        if (result.isFailure) {
        throw Exception("Patch ${metadata.shortName} failed", result.exceptionOrNull()!!)
        } else {
        println("Patch ${metadata.shortName} applied successfully!")
        }
        }
        val out = patcher.save()
        assertTrue(out.isNotEmpty(), "Expected the output of Patcher#save() to not be empty.")
         */
    }
}