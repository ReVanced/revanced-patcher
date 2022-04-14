package app.revanced.patcher

import app.revanced.patcher.signature.PatternScanMethod
import app.revanced.patcher.usage.ExamplePatch
import org.junit.jupiter.api.Test
import java.io.File
import kotlin.test.assertTrue

internal class PatcherTest {
    @Test
    fun testPatcher() {
        val patcher = Patcher(File(PatcherTest::class.java.getResource("/test1.dex")!!.toURI()))
        patcher.addPatches(listOf(ExamplePatch()))
        for (signature in patcher.resolveSignatures()) {
            if (!signature.resolved) {
                throw Exception("Signature ${signature.metadata.name} was not resolved!")
            }
            val patternScanMethod = signature.metadata.patternScanMethod
            if (patternScanMethod is PatternScanMethod.Fuzzy) {
                val warnings = patternScanMethod.warnings
                println("Signature ${signature.metadata.name} had ${warnings.size} warnings!")
                for (warning in warnings) {
                    println(warning.toString())
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
    }
}