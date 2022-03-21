package app.revanced.patcher.resolver

internal data class ScanResult(
    val found: Boolean,
    val startIndex: Int? = 0,
    val endIndex: Int? = 0
)
