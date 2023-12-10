rootProject.name = "revanced-patcher"

buildCache {
    local {
        isEnabled = "CI" !in System.getenv()
    }
}
