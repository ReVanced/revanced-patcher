import org.jetbrains.kotlin.gradle.dsl.abi.ExperimentalAbiValidation

plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.vanniktech.mavenPublish)
}

group = "app.revanced"

kotlin {
    @OptIn(ExperimentalAbiValidation::class)
    abiValidation {
        enabled = true
    }

    jvm()

    sourceSets {
        commonMain.dependencies {
            implementation(libs.multidexlib2)
            implementation(libs.smali)
            implementation(project(":patcher"))
        }
        jvmTest.dependencies {
            implementation(libs.mockk)
            implementation(libs.kotlin.test)
            implementation(project(":tests"))
        }
    }

    compilerOptions {
        freeCompilerArgs.addAll(
            "-Xexplicit-backing-fields",
            "-Xcontext-parameters"
        )
    }
}

tasks {
    named<Test>("jvmTest") {
        useJUnitPlatform()
    }
}

mavenPublishing {
    publishing {
        repositories {
            maven {
                name = "githubPackages"
                url = uri("https://maven.pkg.github.com/revanced/revanced-patcher")
                credentials(PasswordCredentials::class)
            }
        }
    }

    signAllPublications()
    extensions.getByType<SigningExtension>().useGpgCmd()

    coordinates(group.toString(), project.name, version.toString())

    pom {
        name = "ReVanced Patcher Matching API"
        description = "Matching API used by ReVanced."
        inceptionYear = "2022"
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