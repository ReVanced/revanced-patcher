
import com.android.build.api.dsl.androidLibrary
import org.jetbrains.kotlin.gradle.dsl.abi.ExperimentalAbiValidation

plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.android.kotlin.multiplatform.library)
    alias(libs.plugins.vanniktech.mavenPublish)
}

group = "app.revanced"

kotlin {
    @OptIn(ExperimentalAbiValidation::class)
    abiValidation {
        enabled = true
    }

    jvm()

    androidLibrary {
        namespace = "app.revanced.patcher"
        compileSdk =
            libs.versions.android.compileSdk
                .get()
                .toInt()
        minSdk =
            libs.versions.android.minSdk
                .get()
                .toInt()

        withHostTestBuilder {}.configure {}
        withDeviceTestBuilder {
            sourceSetTreeName = "test"
        }
    }

    sourceSets {
        commonMain.dependencies {
            implementation(libs.apktool.lib)
            implementation(libs.kotlin.reflect)
            implementation(libs.multidexlib2)
            implementation(libs.smali)
            implementation(libs.xpp3)
        }

        jvmTest.dependencies {
            implementation(libs.mockk)
            implementation(libs.kotlin.test)
        }
    }

    compilerOptions {
        freeCompilerArgs.addAll(
            "-Xexplicit-backing-fields",
            "-Xcontext-parameters",
        )

        jvmToolchain(17)
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
        name = "ReVanced Patcher"
        description = "Patcher used by ReVanced."
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
