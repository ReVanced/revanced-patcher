rootProject.name = "revanced-patcher"

buildCache {
    local {
        isEnabled = !System.getenv().containsKey("CI")
    }
}
