package net.revanced.patcher
import net.revanced.patcher.patch.Patch
import net.revanced.patcher.signatures.Signature
import net.revanced.patcher.sigscan.SignatureScanner
import org.jf.dexlib2.DexFileFactory
import org.jf.dexlib2.dexbacked.DexBackedDexFile
import org.jf.dexlib2.dexbacked.DexBackedMethod
import org.jf.dexlib2.iface.DexFile
import java.io.File
import kotlin.math.sign

class Patcher(private val dexFilePath: File) {
    private lateinit var patches: MutableList<Patch>;
    private lateinit var methodCache: List<DexBackedMethod>;

    private var dexFile: DexFile = DexFileFactory.loadDexFile(dexFilePath, null);

    fun writeDexFile(path: String) {
        DexFileFactory.writeDexFile(path, dexFile)
    }

    fun addPatch(patch: Patch) {
        patches.add(patch)
    }

    fun addSignatures(vararg signatures: Signature) {
        methodCache = SignatureScanner(dexFile.classes.flatMap { classDef -> classDef.methods }).resolve(signatures)
    }

    fun execute() {
        patches.forEach{ patch ->
            patch.Execute();
        }
    }
}