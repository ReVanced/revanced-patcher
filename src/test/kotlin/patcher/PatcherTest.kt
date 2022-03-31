package patcher

import app.revanced.patcher.Patcher
import app.revanced.patcher.cache.Cache
import app.revanced.patcher.extensions.or
import app.revanced.patcher.patch.Patch
import app.revanced.patcher.patch.PatchResult
import app.revanced.patcher.patch.PatchResultError
import app.revanced.patcher.patch.PatchResultSuccess
import app.revanced.patcher.signature.MethodSignature
import org.jf.dexlib2.AccessFlags
import org.jf.dexlib2.Opcode
import org.jf.dexlib2.builder.instruction.BuilderInstruction35c
import org.jf.dexlib2.immutable.reference.ImmutableMethodReference
import java.io.File

fun main() {
    val signatures = arrayOf(
        MethodSignature(
            "main-method",
            "V",
            AccessFlags.STATIC or AccessFlags.PUBLIC,
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
        File("./"),
        signatures
    )

    val mainMethodPatchViaClassProxy = object : Patch("main-method-patch-via-proxy") {
        override fun execute(cache: Cache): PatchResult {
            val proxy = cache.findClass { classDef ->
                classDef.type.contains("XAdRemover")
            } ?: return PatchResultError("Class 'XAdRemover' could not be found")

            val xAdRemoverClass = proxy.resolve()
            val hideReelMethod = xAdRemoverClass.methods.single { method -> method.name.contains("HideReel") }

            val readSettingsMethodRef = ImmutableMethodReference(
                "Lfi/razerman/youtube/XGlobals;",
                "ReadSettings",
                emptyList(),
                "V"
            )

            val instructions = hideReelMethod.implementation!!.instructions

            val readSettingsInstruction = BuilderInstruction35c(
                Opcode.INVOKE_STATIC,
                0,
                0,
                0,
                0,
                0,
                0,
                readSettingsMethodRef
            )

            // TODO: figure out control flow
            //  otherwise the we would still jump over to the original instruction at index 21 instead to our new one
            instructions.add(
                21,
                readSettingsInstruction
            )
            return PatchResultSuccess()
        }
    }

    val mainMethodPatchViaSignature = object : Patch("main-method-patch-via-signature") {
        override fun execute(cache: Cache): PatchResult {
            val mainMethodMap = cache.resolvedMethods["main-method"]
            mainMethodMap.definingClassProxy.immutableClass.methods.single { method ->
                method.name == mainMethodMap.resolvedMethodName
            }

            return PatchResultSuccess()
        }
    }

    patcher.addPatches(mainMethodPatchViaClassProxy, mainMethodPatchViaSignature)
    patcher.applyPatches()
    patcher.save()
}
