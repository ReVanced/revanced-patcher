package app.revanced.patcher

import app.revanced.patcher.extensions.toInstructions
import app.revanced.patcher.patch.BytecodePatchContext
import app.revanced.patcher.patch.Patch
import app.revanced.patcher.patch.ResourcePatchContext
import com.android.tools.smali.dexlib2.Opcodes
import com.android.tools.smali.dexlib2.iface.DexFile
import com.android.tools.smali.dexlib2.immutable.ImmutableClassDef
import com.android.tools.smali.dexlib2.immutable.ImmutableMethod
import com.android.tools.smali.dexlib2.immutable.ImmutableMethodImplementation
import io.mockk.every
import io.mockk.justRun
import io.mockk.mockk
import io.mockk.mockkStatic
import lanchon.multidexlib2.MultiDexIO
import java.io.File
import java.io.InputStream

abstract class PatcherTestBase {
    protected lateinit var bytecodePatchContext: BytecodePatchContext
    protected lateinit var resourcePatchContext: ResourcePatchContext

    protected fun setupMock(
        method: ImmutableMethod = ImmutableMethod(
            "class",
            "method",
            emptyList(),
            "V",
            0,
            null,
            null,
            ImmutableMethodImplementation(
                2,
                """
                    const-string v0, "Hello, World!"
                    iput-object v0, p0, Ljava/lang/System;->out:Ljava/io/PrintStream;
                    iget-object v0, p0, Ljava/lang/System;->out:Ljava/io/PrintStream;
                    return-void
                    const-string v0, "This is a test."
                    return-object v0
                    invoke-virtual { p0, v0 }, Ljava/io/PrintStream;->println(Ljava/lang/String;)V
                    invoke-static { p0 }, Ljava/lang/System;->currentTimeMillis()J
                    check-cast p0, Ljava/io/PrintStream;
                """.toInstructions(),
                null,
                null
            ),
        ),
    ) {
        resourcePatchContext = mockk<ResourcePatchContext>(relaxed = true)
        bytecodePatchContext = mockk<BytecodePatchContext> bytecodePatchContext@{
            mockkStatic(MultiDexIO::readDexFile)
            every {
                MultiDexIO.readDexFile(
                    any(),
                    any(),
                    any(),
                    any(),
                    any()
                )
            } returns mockk<DexFile> {
                every { classes } returns mutableSetOf(
                    ImmutableClassDef(
                        "class",
                        0,
                        null,
                        null,
                        null,
                        null,
                        null,
                        listOf(method),
                    )
                )
                every { opcodes } returns Opcodes.getDefault()
            }

            every { this@bytecodePatchContext.getProperty("apkFile") } returns mockk<File>()

            every { this@bytecodePatchContext.classDefs } returns ClassDefs().apply {
                javaClass.getDeclaredMethod($$"initializeCache$patcher").apply {
                    isAccessible = true
                }.invoke(this)
            }

            every { get() } returns emptySet()

            justRun { this@bytecodePatchContext["extendWith"](any<InputStream>()) }
        }
    }

    protected operator fun Set<Patch>.invoke() {
        runCatching {
            apply(
                bytecodePatchContext,
                resourcePatchContext
            ) { }
        }.fold(
            { it.dexFiles },
            { it.printStackTrace() }
        )
    }

    protected operator fun Patch.invoke() = setOf(this)()
}