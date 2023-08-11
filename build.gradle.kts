plugins {
    kotlin("jvm") version "1.8.20"
    `maven-publish`
}

group = "app.revanced"

val githubUsername: String = project.findProperty("gpr.user") as? String ?: System.getenv("GITHUB_ACTOR")
val githubPassword: String = project.findProperty("gpr.key") as? String ?: System.getenv("GITHUB_TOKEN")

repositories {
    mavenCentral()
    google()
    listOf("multidexlib2", "apktool").forEach { repo ->
        maven {
            url = uri("https://maven.pkg.github.com/revanced/$repo")
            credentials {
                username = githubUsername
                password = githubPassword
            }
        }
    }
}

dependencies {
    implementation("xpp3:xpp3:1.1.4c")
    implementation("com.android.tools.smali:smali:3.0.3")
    implementation("app.revanced:multidexlib2:3.0.3.r2")
    implementation("app.revanced:apktool-lib:2.8.2-2")

    implementation("org.jetbrains.kotlin:kotlin-reflect:1.8.22")
    testImplementation("org.jetbrains.kotlin:kotlin-test:1.8.20-RC")
}

tasks {
    test {
        useJUnitPlatform()
        testLogging {
            events("PASSED", "SKIPPED", "FAILED")
        }
    }
    processResources {
        expand("projectVersion" to project.version)
    }
}

java {
    withSourcesJar()
}

kotlin {
    jvmToolchain(11)
}

publishing {
    repositories {
        if (System.getenv("GITHUB_ACTOR") != null)
            maven {
                name = "GitHubPackages"
                url = uri("https://maven.pkg.github.com/revanced/revanced-patcher")
                credentials {
                    username = System.getenv("GITHUB_ACTOR")
                    password = System.getenv("GITHUB_TOKEN")
                }
            }
        else
            mavenLocal()
    }
    publications {
        register<MavenPublication>("gpr") {
            from(components["java"])
        }
    }
}
