rootProject.name = "revanced-patcher"

enableFeaturePreview("TYPESAFE_PROJECT_ACCESSORS")

pluginManagement {
    repositories {
        mavenCentral()
        google()
    }
}

dependencyResolutionManagement {
    repositories {
        mavenCentral()
        google()
        maven {
            name = "githubPackages"
            // A repository must be specified for some reason. "registry" is a dummy.
            url = uri("https://maven.pkg.github.com/revanced/registry")
            credentials(PasswordCredentials::class)
        }
    }
}

include(":patcher")
