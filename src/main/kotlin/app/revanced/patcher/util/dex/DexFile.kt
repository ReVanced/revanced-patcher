package app.revanced.patcher.util.dex

import org.jf.dexlib2.writer.io.MemoryDataStore

/**
 * Wrapper for dex files.
 * @param name The original name of the dex file
 * @param memoryDataStore The data store for the dex file.
 */
data class DexFile(val name: String, val memoryDataStore: MemoryDataStore)