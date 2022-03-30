package app.revanced.patcher

import app.revanced.patcher.cache.Cache
import app.revanced.patcher.patch.Patch
import app.revanced.patcher.patch.PatchResult
import app.revanced.patcher.patch.PatchResultError
import app.revanced.patcher.patch.PatchResultSuccess
import app.revanced.patcher.signature.MethodSignature
import org.jf.dexlib2.AccessFlags
import org.jf.dexlib2.Opcode
import org.jf.dexlib2.builder.instruction.BuilderInstruction21c
import org.jf.dexlib2.iface.instruction.formats.Instruction21c
import org.jf.dexlib2.iface.reference.FieldReference
import org.jf.dexlib2.immutable.reference.ImmutableMethodReference
import java.io.File


fun main() {
    val signatures = arrayOf(
        MethodSignature(
            "main-method",
            "V",
            AccessFlags.STATIC.value or AccessFlags.PUBLIC.value,
            listOf("[O"),
            arrayOf(
                Opcode.SGET_OBJECT,
                Opcode.CONST_STRING,
                Opcode.INVOKE_VIRTUAL,
                Opcode.RETURN_VOID
            )
        )
    )

    val patcher = Patcher(
        File("black.apk"),
        File("folder/"),
        signatures
    )

    val mainMethodPatchViaClassProxy = object : Patch("main-method-patch-via-proxy") {
        override fun execute(cache: Cache): PatchResult {
            val proxy = cache.findClass { classDef ->
                classDef.methods.any { method ->
                    method.name == "main"
                }
            } ?: return PatchResultError("Class with method 'mainMethod' could not be found")

            val mainMethodClass = proxy.resolve()
            val mainMethod = mainMethodClass.methods.single { method -> method.name == "main" }

            val hideReelMethodRef = ImmutableMethodReference(
                "Lfi/razerman/youtube/XAdRemover;",
                "HideReel",
                listOf("Landroid/view/View;"),
                "V"
            )

            val mainMethodInstructions = mainMethod.implementation!!.instructions
            val printStreamFieldRef = (mainMethodInstructions.first() as Instruction21c).reference as FieldReference
            // TODO: not sure how to use the registers yet, find a way
            mainMethodInstructions.add(BuilderInstruction21c(Opcode.SGET_OBJECT, 0, printStreamFieldRef))
            return PatchResultSuccess()
        }
    }

    val mainMethodPatchViaSignature = object : Patch("main-method-patch-via-signature") {
        override fun execute(cache: Cache): PatchResult {
            cache.resolvedMethods["main-method"].method
            return  PatchResultSuccess()
        }
    }
    patcher.addPatches(mainMethodPatchViaClassProxy, mainMethodPatchViaSignature)
    patcher.applyPatches()
    patcher.save()
}