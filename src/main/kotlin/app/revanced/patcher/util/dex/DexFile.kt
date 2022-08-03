package app.revanced.patcher.util.dex

import java.io.InputStream

/**
 * Wrapper for dex files.
 * @param name The original name of the dex file.
 * @param stream The dex file as [InputStream].
 */
data class DexFile(val name: String, val stream: InputStream)