plugins {
    kotlin("jvm")
    `maven-publish`
}

group = "app.revanced"

dependencies {
    implementation("io.github.reandroid:ARSCLib:1.1.7")
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