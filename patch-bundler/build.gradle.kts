plugins {
    kotlin("jvm") version "1.7.0"
    `java-gradle-plugin`
}

group = "app.revanced"

dependencies {
    api(project(":"))

    testImplementation(kotlin("test"))
}

gradlePlugin {
    @Suppress("UNUSED_VARIABLE") val bundle by plugins.creating {
        id = "app.revanced.patchbundler"
        implementationClass = "app.revanced.patchbundler.PatchBundlerPlugin"
    }
}

gradlePlugin.testSourceSets(sourceSets.test.get())

tasks.check {
    // Run the tests as part of `check`
    dependsOn(tasks.test)
}
