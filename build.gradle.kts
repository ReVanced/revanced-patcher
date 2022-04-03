plugins {
    kotlin("jvm") version "1.6.10"
    java
    `maven-publish`
}

group = "app.revanced"

repositories {
    mavenCentral()
}

dependencies {
    implementation(kotlin("stdlib"))

    implementation("com.github.lanchon.dexpatcher:multidexlib2:2.3.4.r2")
    implementation("org.smali:smali:2.3.4")

    testImplementation(kotlin("test"))
}

tasks.test {
    useJUnitPlatform()
    testLogging {
        events("PASSED", "SKIPPED", "FAILED")
    }
}

java {
    withSourcesJar()
    withJavadocJar()
}

publishing {
    repositories {
        maven {
            name = "GitHubPackages"
            url = uri("https://maven.pkg.github.com/ReVancedTeam/revanced-patcher")
            credentials {
                username = System.getenv("GITHUB_ACTOR")
                password = System.getenv("GITHUB_TOKEN")
            }
        }
    }
    publications {
        register<MavenPublication>("gpr") {
            from(components["java"])
        }
    }
}
