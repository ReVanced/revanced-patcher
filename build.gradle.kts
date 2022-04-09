plugins {
    kotlin("jvm") version "1.6.20"
    java
    `maven-publish`
}

group = "app.revanced"

repositories {
    mavenCentral()
    maven {
        url = uri("https://maven.pkg.github.com/ReVancedTeam/multidexlib2")
        credentials {
            // DO NOT set these variables in the project's gradle.properties.
            // Instead, you should set them in:
            // Windows: %homepath%\.gradle\gradle.properties
            // Linux: ~/.gradle/gradle.properties
            username = project.findProperty("gpr.user") as String? ?: System.getenv("GITHUB_ACTOR") // DO NOT CHANGE!
            password = project.findProperty("gpr.key") as String? ?: System.getenv("GITHUB_TOKEN") // DO NOT CHANGE!
        }
    }
}

dependencies {
    implementation(kotlin("stdlib"))

    api("app.revanced:multidexlib2:2.5.2.r2")
    @Suppress("GradlePackageUpdate")
    api("org.smali:smali:2.5.2")

    testImplementation(kotlin("test"))
}

tasks {
    test {
        useJUnitPlatform()
        testLogging {
            events("PASSED", "SKIPPED", "FAILED")
        }
    }
}

java {
    withSourcesJar()
    withJavadocJar()
}

val isGitHubCI = System.getenv("GITHUB_ACTOR") != null

publishing {
    repositories {
        if (isGitHubCI) {
            maven {
                name = "GitHubPackages"
                url = uri("https://maven.pkg.github.com/ReVancedTeam/revanced-patcher")
                credentials {
                    username = System.getenv("GITHUB_ACTOR")
                    password = System.getenv("GITHUB_TOKEN")
                }
            }
        } else {
            mavenLocal()
        }
    }
    publications {
        register<MavenPublication>("gpr") {
            from(components["java"])
        }
    }
}
