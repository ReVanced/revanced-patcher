import org.jetbrains.kotlin.gradle.dsl.KotlinJvmProjectExtension

plugins {
    kotlin("jvm") version "1.9.0"
    alias(libs.plugins.binary.compatibility.validator)
    `maven-publish`
    signing
    java
}

val githubUsername: String = providers.gradleProperty("gpr.user").orNull ?: System.getenv("GITHUB_ACTOR")
val githubPassword: String = providers.gradleProperty("gpr.key").orNull ?: System.getenv("GITHUB_TOKEN")

val isDev = project.version.toString().contains("-dev")

var publicationVersion = project.version.toString()
if (isDev) publicationVersion += "-SNAPSHOT"

subprojects {
    apply(plugin = "maven-publish")
    apply(plugin = "signing")
    apply(plugin = "java")
    apply(plugin ="kotlin")

    group = "app.revanced"

    extra["version"] = version

    repositories {
        mavenCentral()
        mavenLocal()
        google()
        listOf("multidexlib2", "apktool").forEach { repo ->
            maven {
                url = uri("https://maven.pkg.github.com/revanced/$repo")
                credentials {
                    username = githubUsername
                    password = githubPassword
                }
            }
        }
    }

    java {
        withJavadocJar()
        withSourcesJar()
    }

    configure<KotlinJvmProjectExtension> {
        kotlin { jvmToolchain(11) }
    }

    signing {
        useGpgCmd()
        sign(publishing.publications)
    }

    publishing {
        repositories {
            mavenLocal()
            maven {
                url = if (isDev)
                    uri("https://s01.oss.sonatype.org/content/repositories/snapshots/")
                else
                    uri("https://s01.oss.sonatype.org/service/local/staging/deploy/maven2/")

                credentials {
                    username = (System.getenv("OSSRH_USERNAME") ?: "").toString()
                    password = (System.getenv("OSSRH_PASSWORD") ?: "").toString()
                }
            }
        }
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
