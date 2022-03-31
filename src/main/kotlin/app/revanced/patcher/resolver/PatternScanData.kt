package app.revanced.patcher.resolver

internal data class PatternScanData(
    val found: Boolean,
    val startIndex: Int? = 0,
    val endIndex: Int? = 0
)