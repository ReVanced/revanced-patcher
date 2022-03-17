package net.revanced.patcher.signatures.v17_03_38

import net.revanced.patcher.signatures.SignatureSupplier
import net.revanced.patcher.signatures.ElementType
import net.revanced.patcher.signatures.Signature
import java.lang.reflect.Modifier

import org.jf.dexlib2.Opcode.*

class Sigs: SignatureSupplier {
    override fun signatures(): Array<Signature> {
        return arrayOf(
            // public static aT(Landroid/content/Context;I)Z
            Signature(
                arrayOf(
                    IF_LT,   // if-lt p0, p1, :cond_1
                    CONST_4, // const/4 p0, 0x1
                    // TODO(Inject):
                    // invoke-static {p0}, Lfi/razerman/youtube/XGlobals;->getOverride(Z)Z
                    // move-result p0
                    RETURN,  // return p0
                             // :cond_1
                    CONST_4, // const/4 p0, 0x0
                    // TODO(Inject):
                    // invoke-static {p0}, Lfi/razerman/youtube/XGlobals;->getOverride(Z)Z
                    // move-result p0
                    RETURN,  // return p0
                ),
                Modifier.PUBLIC or Modifier.STATIC,
                ElementType.Boolean
            )
        )
    }
}