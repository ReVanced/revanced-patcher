plugins {
    kotlin("jvm") version "1.9.0"
    `maven-publish`
}

group = "app.revanced"

dependencies {
    implementation(libs.kotlinx.coroutines.core)
    implementation(libs.xpp3)
    implementation(libs.smali)
    implementation(libs.multidexlib2)
    implementation(libs.apktool.lib)
    implementation(libs.kotlin.reflect)

    compileOnly(libs.android)

    testImplementation(project(":revanced-patch-annotations-processor"))
    testImplementation(libs.kotlin.test)
}

tasks {
    test {
        useJUnitPlatform()
        testLogging {
            events("PASSED", "SKIPPED", "FAILED")
        }
    }

    processResources {
        expand("projectVersion" to project.version)
    }
}

kotlin { jvmToolchain(11) }

java {
    withSourcesJar()
}

publishing {
    repositories {
        mavenLocal()
        maven {
            name = "GitHubPackages"
            url = uri("https://maven.pkg.github.com/revanced/revanced-patcher")
            credentials {
                username = System.getenv("GITHUB_ACTOR")
                password = System.getenv("GITHUB_TOKEN")
            }
        }
    }
    publications {
        create<MavenPublication>("gpr") {
            from(components["java"])
        }
    }
}
