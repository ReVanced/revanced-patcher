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
    processResources {
        expand("projectVersion" to project.version)
    }
}

publishing {
    publications {
        create<MavenPublication>("revanced-patcher-publication") {
            from(components["java"])

            version = extra["version"].toString()

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
