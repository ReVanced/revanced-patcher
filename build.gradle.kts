plugins {
    kotlin("jvm") version "1.9.0" apply false
    alias(libs.plugins.binary.compatibility.validator)
}

allprojects {
    apply(plugin = "maven-publish")

    group = "app.revanced"
}