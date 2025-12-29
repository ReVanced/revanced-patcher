import com.android.build.api.dsl.androidLibrary
import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.jetbrains.kotlin.gradle.dsl.abi.ExperimentalAbiValidation

plugins {
    alias(libs.plugins.kotlinMultiplatform)
}

group = "app.revanced"

kotlin {
    jvm()

    sourceSets {
        commonMain.dependencies {
            implementation(libs.kotlinx.coroutines.core)
            implementation(libs.multidexlib2)
            implementation(libs.smali)
            implementation(project(":patcher"))
            implementation(libs.mockk)
            implementation(libs.kotlin.test)
        }
    }
}
