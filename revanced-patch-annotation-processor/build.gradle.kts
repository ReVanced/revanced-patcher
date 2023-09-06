plugins {
    kotlin("jvm") version "1.9.0"
    alias(libs.plugins.ksp)
}

dependencies {
    implementation(libs.symbol.processing.api)
    implementation(libs.kotlinpoet.ksp)
    implementation(project(":revanced-patcher"))

    testImplementation(libs.kotlin.test)
    testImplementation(libs.kotlin.compile.testing)
}

tasks {
    test {
        useJUnitPlatform()
        testLogging {
            events("PASSED", "SKIPPED", "FAILED")
        }
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

            version = project.version.toString()

            pom {
                name = "ReVanced patch annotation processor"
                description = "Annotation processor for patches."
                url = "https://revanced.app"

                licenses {
                    license {
                        name = "GNU General Public License v3.0"
                        url = "https://www.gnu.org/licenses/gpl-3.0.en.html"
                    }
                }
                developers {
                    developer {
                        id = "ReVanced"
                        name = "ReVanced"
                        email = "contact@revanced.app"
                    }
                }
                scm {
                    connection = "scm:git:git://github.com/revanced/revanced-patcher.git"
                    developerConnection = "scm:git:git@github.com:revanced/revanced-patcher.git"
                    url = "https://github.com/revanced/revanced-patcher"
                }
            }
        }
    }
}