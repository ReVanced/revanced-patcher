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

                val instructions = hideReelMethod.implementation!!

                val readInsn =
                    "invoke-static { }, Lfi/razerman/youtube/XGlobals;->ReadSettings()V"
                        .asInstruction() as BuilderInstruction35c
                val testInsn = BuilderInstruction35c(
                    Opcode.INVOKE_STATIC,
                    0, 0, 0, 0, 0, 0,
                    ImmutableMethodReference(
                        "Lfi/razerman/youtube/XGlobals;",
                        "ReadSettings",
                        emptyList(),
                        "V"
                    )
                )

                assertEquals(testInsn.opcode, readInsn.opcode)
                assertEquals(testInsn.referenceType, readInsn.referenceType)
                assertEquals(testInsn.registerCount, readInsn.registerCount)
                assertEquals(testInsn.registerC, readInsn.registerC)
                assertEquals(testInsn.registerD, readInsn.registerD)
                assertEquals(testInsn.registerE, readInsn.registerE)
                assertEquals(testInsn.registerF, readInsn.registerF)
                assertEquals(testInsn.registerG, readInsn.registerG)
                run {
                    val tref = testInsn.reference as MethodReference
                    val rref = readInsn.reference as MethodReference

                    assertEquals(tref.name, rref.name)
                    assertEquals(tref.definingClass, rref.definingClass)
                    assertEquals(tref.returnType, rref.returnType)
                    assertContentEquals(tref.parameterTypes, rref.parameterTypes)
                }

                // TODO: figure out control flow
                //  otherwise the we would still jump over to the original instruction at index 21 instead to our new one
                instructions.addInstruction(
                    21,
                    readInsn
                )
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
