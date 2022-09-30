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
    implementation("app.revanced:smali:2.5.3-a3836654")
    implementation("app.revanced:multidexlib2:2.5.3-a3836654")
    implementation("app.revanced:apktool-lib:2.7.0")
    implementation(kotlin("reflect"))

    compileOnly("com.google.android:android:4.1.1.4")

    testImplementation(kotlin("test"))
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
