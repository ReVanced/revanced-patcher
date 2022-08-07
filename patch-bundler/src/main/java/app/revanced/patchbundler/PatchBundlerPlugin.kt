package app.revanced.patchbundler

import app.revanced.patcher.util.patch.bundle.PatchBundle
import app.revanced.patcher.util.patch.bundle.PatchBundleFormat
import app.revanced.patcher.util.patch.bundle.PatchResource
import app.revanced.patcher.util.patch.bundle.PatchResourceType
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.tasks.bundling.Jar
import java.io.File
import java.nio.file.Files

@OptIn(ExperimentalStdlibApi::class)
class PatchBundlerPlugin : Plugin<Project> {
    override fun apply(project: Project) {
        val jarTask = project.tasks.named("jar", Jar::class).get()

        project.tasks.register("bundle") { task ->
            task.dependsOn(jarTask)
            task.doLast {
                val archive = jarTask.archiveFile.get().asFile
                val finalPath = File(archive.parentFile, "${archive.nameWithoutExtension}.rvbundle").toPath()
                val resourceDir = project.sourceSets.getByName("main").output.resourcesDir!!

                val now = System.currentTimeMillis()
                val bytes = PatchBundleFormat.serialize(
                    PatchBundle.Metadata("a", "b", "c"),
                    buildList {
                        add(PatchResource.fromFile(archive, PatchResourceType.JAR))
                        addAll(gatherResources(resourceDir))
                    }
                )
                Files.write(finalPath, bytes)

                println("Saved to $finalPath in ${System.currentTimeMillis() - now}ms")
            }
        }
    }

    private fun gatherResources(resourceDir: File) = buildList {
        val resourcePath = resourceDir.toPath()
        if (!Files.exists(resourcePath)) return@buildList
        Files.walk(resourcePath).use { stream ->
            for (path in stream) {
                if (!Files.isRegularFile(path)) continue
                add(PatchResource.fromFile(path.toFile().relativeTo(resourceDir), PatchResourceType.RESOURCE))
            }
        }
    }
}