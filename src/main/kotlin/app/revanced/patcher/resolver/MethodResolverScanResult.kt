package app.revanced.patcher.resolver

internal data class MethodResolverScanResult(
    val found: Boolean,
    val startIndex: Int? = 0,
    val endIndex: Int? = 0
)