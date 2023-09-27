plugins {
    kotlin("jvm") version "1.9.0"
}

dependencies {
    implementation(libs.kotlinx.coroutines.core)
    implementation(libs.xpp3)
    implementation(libs.smali)
    implementation(libs.multidexlib2)
    implementation(libs.apktool.lib)
    implementation(libs.kotlin.reflect)

    compileOnly(libs.android)

    testImplementation(project(":revanced-patch-annotation-processor"))
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

            version = project.version.toString()

            pom {
                name = "ReVanced Patcher"
                description = "Patcher used by ReVanced."
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
