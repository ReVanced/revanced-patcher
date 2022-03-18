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
    testImplementation(kotlin("test"))

    implementation("org.ow2.asm:asm:9.2")
}

tasks.test {
    useJUnitPlatform()
}
