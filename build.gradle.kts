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
    implementation(kotlin("stdlib"))

    api("xpp3:xpp3:1.1.4c")
    api("org.apktool:apktool-lib:2.6.3-SNAPSHOT")
    api("app.revanced:multidexlib2:2.5.2.r2")
    api("org.smali:smali:2.5.2")

    testImplementation(kotlin("test"))
    implementation(kotlin("reflect"))}

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
