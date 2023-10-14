import org.jetbrains.kotlin.gradle.dsl.KotlinJvmProjectExtension

plugins {
    kotlin("jvm") version "1.9.0"
    alias(libs.plugins.binary.compatibility.validator)
    `maven-publish`
    signing
    java
}

val publicationVersion = project.version.toString()

subprojects {
    apply(plugin = "maven-publish")
    apply(plugin = "signing")
    apply(plugin = "java")
    apply(plugin ="kotlin")

    version = pulicationVersion

    repositories {
        mavenCentral()
        mavenLocal()
        maven { url = uri("https://jitpack.io") }
        google()
    }

    java {
        withJavadocJar()
        withSourcesJar()
    }

    configure<KotlinJvmProjectExtension> {
        kotlin { jvmToolchain(11) }
    }

    tasks {
        test {
            useJUnitPlatform()
            testLogging {
                events("PASSED", "SKIPPED", "FAILED")
            }
        }
    }
}
