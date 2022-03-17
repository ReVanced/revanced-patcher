package net.revanced.patcher

import net.revanced.patcher.version.YTVersion
import org.jf.dexlib2.DexFileFactory
import java.io.File

/**
 * Creates a patcher.
 *
 * @param input the input dex file
 * @param output the output dex file
 * @param version the YT version of this dex file
 */
class Patcher(private val input: File, private val output: File, private val version: YTVersion) {
    // setting opcodes to null causes it to autodetect, perfect!
    private val dexFile = DexFileFactory.loadDexFile(input, null)

    /**
     * Runs the patcher.
     */
    fun run() {

    }
}


