package app.revanced.patcher.util.dex

import java.io.InputStream

/**
 * Wrapper for dex files.
 * @param name The original name of the dex file.
 * @param dexFileInputStream The dex file as [InputStream].
 */
data class DexFile(val name: String, val dexFileInputStream: InputStream)