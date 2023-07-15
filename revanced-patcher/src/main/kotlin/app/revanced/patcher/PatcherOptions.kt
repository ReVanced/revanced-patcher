package app.revanced.patcher

import app.revanced.patcher.apk.ApkBundle
import app.revanced.patcher.logging.Logger

/**
 * Options for the [Patcher].
 * @param apkBundle The [ApkBundle].
 * @param logger Custom logger implementation for the [Patcher].
 */
class PatcherOptions(
    internal val apkBundle: ApkBundle,
    internal val logger: Logger = Logger.Nop
)