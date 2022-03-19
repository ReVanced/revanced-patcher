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
    implementation("org.ow2.asm:asm:9.2")
    implementation("org.ow2.asm:asm-util:9.2")
    implementation("org.ow2.asm:asm-tree:9.2")
    implementation("org.ow2.asm:asm-commons:9.2")
    testImplementation(kotlin("test"))
}

tasks.test {
    useJUnitPlatform()
}
