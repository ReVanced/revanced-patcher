plugins {
    kotlin("jvm") version "1.7.0"
    java
    `maven-publish`
}

group = "app.revanced"

val githubUsername: String = project.findProperty("gpr.user") as? String ?: System.getenv("GITHUB_ACTOR")
val githubPassword: String = project.findProperty("gpr.key") as? String ?: System.getenv("GITHUB_TOKEN")

repositories {
    mavenCentral()
    maven {
        url = uri("https://maven.pkg.github.com/revanced/multidexlib2")
        credentials {
            username = githubUsername
            password = githubPassword
        }
    }
}

dependencies {
    implementation("xpp3:xpp3:1.1.4c")
    implementation("app.revanced:multidexlib2:2.5.2.r2")
    implementation("org.smali:smali:2.5.2")
    implementation("org.apktool:apktool-lib:2.6.5-SNAPSHOT")
    implementation("org.slf4j:slf4j-api:1.7.36")

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
}

publishing {
    repositories {
            maven {
                name = "GitHubPackages"
                url = uri("https://maven.pkg.github.com/revanced/revanced-patcher")
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
