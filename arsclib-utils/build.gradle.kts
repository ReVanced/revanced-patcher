plugins {
    kotlin("jvm")
    `maven-publish`
}

group = "app.revanced"

dependencies {
    implementation("io.github.reandroid:ARSCLib:1.1.7")
}

java {
    withSourcesJar()
}

kotlin {
    jvmToolchain(11)
}