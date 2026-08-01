package app.revanced.patcher.patch

import com.android.tools.smali.dexlib2.AccessFlags
import com.android.tools.smali.dexlib2.Opcodes
import com.android.tools.smali.dexlib2.iface.ClassDef
import com.android.tools.smali.dexlib2.iface.DexFile
import com.android.tools.smali.dexlib2.immutable.ImmutableClassDef
import io.mockk.unmockkStatic
import lanchon.multidexlib2.DexIO
import lanchon.multidexlib2.MultiDexIO
import lanchon.multidexlib2.RawDexIO
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.io.TempDir
import java.io.File
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream
import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals

internal class BytecodePatchContextTest {
    @TempDir
    lateinit var temporaryFilesPath: File

    // Other tests mock MultiDexIO statically, which leaks into this test.
    @BeforeEach
    fun unmockMultiDexIO() = unmockkStatic(MultiDexIO::class)

    @Test
    fun `reuses all dex files if no class was modified`() {
        val (context, dexFiles) = setupContext()

        assertEquals(
            dexFiles.mapValues { (_, bytes) -> bytes.toList() },
            context.get().associate { it.name to it.stream.readBytes().toList() },
            "Every dex file should be reused as-is.",
        )
    }

    @Test
    fun `reuses the dex files of which no class was modified`() {
        val (context, dexFiles) = setupContext()

        context.classDefs.initializeCache()
        context.classDefs.getOrReplaceMutable(context.classDefs["LSecond;"]!!)

        val patchedDexFiles = context.get().associate { it.name to it.stream.readBytes() }

        assertEquals(
            setOf("classes.dex", "classes2.dex"),
            patchedDexFiles.keys,
            "The dex files should be named without gaps in their numbering.",
        )

        assertContentEquals(
            dexFiles["classes.dex"],
            patchedDexFiles["classes.dex"],
            "The dex file of which no class was modified should be reused as-is.",
        )

        assertEquals(
            listOf("LSecond;"),
            readDexFile(patchedDexFiles["classes2.dex"]!!).classes.map { it.type },
            "The modified class should have been compiled into a new dex file.",
        )
    }

    /**
     * Set up a [BytecodePatchContext] for an apk containing two dex files, each containing a single class.
     *
     * @return The context and the dex files of the apk mapped by their name.
     */
    private fun setupContext(): Pair<BytecodePatchContext, Map<String, ByteArray>> {
        val dexFiles =
            listOf("classes.dex" to "LFirst;", "classes2.dex" to "LSecond;").associate { (name, type) ->
                val dexFile = temporaryFilesPath.resolve(name)

                RawDexIO.writeRawDexFile(
                    dexFile,
                    dexFileOf(
                        ImmutableClassDef(
                            type,
                            AccessFlags.PUBLIC.value,
                            "Ljava/lang/Object;",
                            null,
                            null,
                            null,
                            null,
                            null,
                        ),
                    ),
                    DexIO.DEFAULT_MAX_DEX_POOL_SIZE,
                )

                name to dexFile.readBytes()
            }

        val apkFile =
            temporaryFilesPath.resolve("apk.apk").apply {
                ZipOutputStream(outputStream()).use { zipOutputStream ->
                    dexFiles.forEach { (name, bytes) ->
                        zipOutputStream.putNextEntry(ZipEntry(name))
                        zipOutputStream.write(bytes)
                        zipOutputStream.closeEntry()
                    }
                }
            }

        return BytecodePatchContext(apkFile, temporaryFilesPath.resolve("patched")) to dexFiles
    }

    private fun dexFileOf(vararg classDefs: ClassDef) =
        object : DexFile {
            override fun getClasses() = classDefs.toSet()

            override fun getOpcodes() = Opcodes.getDefault()
        }

    private fun readDexFile(bytes: ByteArray) = RawDexIO.readRawDexFile(bytes, 0, null)
}
