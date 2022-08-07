package app.revanced.patchbundler

import org.gradle.testkit.runner.GradleRunner
import org.junit.Test
import java.io.File
import java.io.FileWriter
import java.nio.file.Files

internal class PatchBundlerPluginTest {
    @Test
    fun canRunTask() {
        // Set up the test build
        val projectDir = File("build/test")
        Files.createDirectories(projectDir.toPath())
        writeString(File(projectDir, "settings.gradle"), "")
        writeString(
            File(projectDir, "build.gradle"),
            """
                plugins {
                    id('org.gradle.java')
                    id('app.revanced.patchbundler')
                }
            """.trimIndent()
        )

        // Run the build
        GradleRunner.create()
            .forwardOutput()
            .withPluginClasspath()
            .withArguments("bundle", "--stacktrace")
            .withProjectDir(projectDir)
            .build()
    }

    private fun writeString(file: File, s: String) {
        FileWriter(file).use { it.write(s) }
    }
}