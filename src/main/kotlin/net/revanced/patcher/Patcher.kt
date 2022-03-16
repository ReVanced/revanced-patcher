package net.revanced.patcher

import net.revanced.patcher.sigscan.Sig
import net.revanced.patcher.sigscan.SigScanner
import org.jf.dexlib2.Opcode
import java.io.File
import java.lang.reflect.Modifier

class Patcher {
    companion object {
        /**
         * Invokes the patcher on the given input file.
         *
         * @param input the input file
         * @param output the output file
         */
        fun invoke(input: File, output: File) {
            SigScanner(Sig(
                arrayOf(Opcode.ADD_INT),
                Modifier.PUBLIC or Modifier.STATIC,
                String.Companion::class
            )).scan(emptyArray())
        }
    }
}
