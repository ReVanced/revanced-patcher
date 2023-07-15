plugins {
    kotlin("jvm")
    `maven-publish`
}

group = "app.revanced"

dependencies {
    implementation("xpp3:xpp3:1.1.4c")
    implementation("app.revanced:smali:2.5.3-a3836654")
    implementation("app.revanced:multidexlib2:2.5.3-a3836654")
    implementation("io.github.reandroid:ARSCLib:1.1.7")
    implementation(project(":arsclib-utils"))

    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.7.1")
    implementation("org.jetbrains.kotlin:kotlin-reflect:1.8.20-RC")
    testImplementation("org.jetbrains.kotlin:kotlin-test:1.8.20-RC")

    compileOnly("com.google.android:android:4.1.1.4")
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
