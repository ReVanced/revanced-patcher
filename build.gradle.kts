plugins {
    kotlin("jvm") version "1.6.10"
    java
}

group = "net.revanced"
version = "1.0"

repositories {
    mavenCentral()
}

dependencies {
    implementation(kotlin("stdlib"))

    implementation("org.smali:dexlib2:2.5.2")
}