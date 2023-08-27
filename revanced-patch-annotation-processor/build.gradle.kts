plugins {
    alias(libs.plugins.ksp)
}

dependencies {
    implementation(libs.symbol.processing.api)
    implementation(libs.kotlinpoet.ksp)
    implementation(project(":revanced-patcher"))

    testImplementation(libs.kotlin.test)
    testImplementation(libs.kotlin.compile.testing)
}

publishing {
    publications {
        create<MavenPublication>("revanced-patch-annotation-processor-publication") {
            from(components["java"])

            version = extra["version"].toString()

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