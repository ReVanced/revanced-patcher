package patcher

import app.revanced.patcher.Patcher
import app.revanced.patcher.cache.Cache
import app.revanced.patcher.extensions.or
import app.revanced.patcher.patch.Patch
import app.revanced.patcher.patch.PatchResult
import app.revanced.patcher.patch.PatchResultError
import app.revanced.patcher.patch.PatchResultSuccess
import app.revanced.patcher.signature.MethodSignature
import app.revanced.patcher.smali.asInstruction
import org.jf.dexlib2.AccessFlags
import org.jf.dexlib2.Opcode
import org.jf.dexlib2.builder.instruction.BuilderInstruction21t
import org.jf.dexlib2.builder.instruction.BuilderInstruction35c
import org.jf.dexlib2.iface.reference.MethodReference
import org.jf.dexlib2.immutable.reference.ImmutableMethodReference
import java.io.File
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals

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

    patcher.addPatches(
        object : Patch("main-method-patch-via-proxy") {
            override fun execute(cache: Cache): PatchResult {
                val proxy = cache.findClass("XAdRemover")
                    ?: return PatchResultError("Class 'XAdRemover' could not be found")

                val xAdRemoverClass = proxy.resolve()
                val hideReelMethod = xAdRemoverClass.methods.find {
                    it.name.contains("HideReel")
                }!!

                val implementation = hideReelMethod.implementation!!

                val readSettingsInstructionCompiled =
                    "invoke-static { }, Lfi/razerman/youtube/XGlobals;->ReadSettings()V"
                        .asInstruction() as BuilderInstruction35c
                val readSettingsInstructionAssembled = BuilderInstruction35c(
                    Opcode.INVOKE_STATIC,
                    0, 0, 0, 0, 0, 0,
                    ImmutableMethodReference(
                        "Lfi/razerman/youtube/XGlobals;",
                        "ReadSettings",
                        emptyList(),
                        "V"
                    )
                )

                assertEquals(readSettingsInstructionAssembled.opcode, readSettingsInstructionCompiled.opcode)
                assertEquals(
                    readSettingsInstructionAssembled.referenceType,
                    readSettingsInstructionCompiled.referenceType
                )
                assertEquals(
                    readSettingsInstructionAssembled.registerCount,
                    readSettingsInstructionCompiled.registerCount
                )
                assertEquals(readSettingsInstructionAssembled.registerC, readSettingsInstructionCompiled.registerC)
                assertEquals(readSettingsInstructionAssembled.registerD, readSettingsInstructionCompiled.registerD)
                assertEquals(readSettingsInstructionAssembled.registerE, readSettingsInstructionCompiled.registerE)
                assertEquals(readSettingsInstructionAssembled.registerF, readSettingsInstructionCompiled.registerF)
                assertEquals(readSettingsInstructionAssembled.registerG, readSettingsInstructionCompiled.registerG)
                run {
                    val compiledRef = readSettingsInstructionCompiled.reference as MethodReference
                    val assembledRef = readSettingsInstructionAssembled.reference as MethodReference

                    assertEquals(assembledRef.name, compiledRef.name)
                    assertEquals(assembledRef.definingClass, compiledRef.definingClass)
                    assertEquals(assembledRef.returnType, compiledRef.returnType)
                    assertContentEquals(assembledRef.parameterTypes, compiledRef.parameterTypes)
                }

                implementation.addInstruction(
                    21,
                    readSettingsInstructionCompiled
                )

                // fix labels
                // create a new label for the instruction we want to jump to
                val newLabel = implementation.newLabelForIndex(21)
                // replace all instances of the old label with the new one
                implementation.replaceInstruction(4, BuilderInstruction21t(Opcode.IF_NEZ, 0, newLabel))
                return PatchResultSuccess()
            }
        },
        object : Patch("main-method-patch-via-signature") {
            override fun execute(cache: Cache): PatchResult {
                val mainMethodMap = cache.resolvedMethods["main-method"]
                mainMethodMap.definingClassProxy.immutableClass.methods.single { method ->
                    method.name == mainMethodMap.resolvedMethodName
                }
                return PatchResultSuccess()
            }
        }
    )

    patcher.applyPatches()
    patcher.save()
}
