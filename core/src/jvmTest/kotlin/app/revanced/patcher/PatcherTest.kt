package app.revanced.patcher

import app.revanced.patcher.extensions.toInstructions
import app.revanced.patcher.patch.*
import com.android.tools.smali.dexlib2.Opcode
import com.android.tools.smali.dexlib2.Opcodes
import com.android.tools.smali.dexlib2.iface.DexFile
import com.android.tools.smali.dexlib2.iface.instruction.TwoRegisterInstruction
import com.android.tools.smali.dexlib2.immutable.ImmutableClassDef
import com.android.tools.smali.dexlib2.immutable.ImmutableMethod
import com.android.tools.smali.dexlib2.immutable.ImmutableMethodImplementation
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.runs
import io.mockk.spyk
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.runBlocking
import lanchon.multidexlib2.MultiDexIO
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.assertAll
import org.junit.jupiter.api.assertThrows
import java.util.logging.Logger
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull

internal object PatcherTest {
    private lateinit var patcher: Patcher
    private lateinit var patcherContext: PatcherContext

    @JvmStatic
    @BeforeAll
    fun setUp() {
        patcher = mockk<Patcher> {
            // Can't mock private fields, until https://github.com/mockk/mockk/issues/1244 is resolved.
            setPrivateField(
                "config",
                mockk<PatcherConfig> {
                    every { resourceMode } returns ResourcePatchContext.ResourceMode.NONE
                },
            )
            setPrivateField(
                "logger",
                Logger.getAnonymousLogger(),
            )

            every { this@mockk() } answers { callOriginal() }
        }

        val classDefs = mutableSetOf(
            ImmutableClassDef(
                "class",
                0,
                null,
                null,
                null,
                null,
                null,
                listOf(
                    ImmutableMethod(
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
                ),
            )
        )

        patcherContext = mockk<PatcherContext> {
            every { bytecodeContext } returns mockk<BytecodePatchContext> context@{
                every { config } returns mockk<PatcherConfig> {
                    every { apkFile } returns mockk()
                }

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
                    every { classes } returns classDefs
                    every { opcodes } returns Opcodes.getDefault()
                }
                every { this@context.classDefs } returns ClassDefs().apply { initializeCache() }
                every { mergeExtension(any<BytecodePatch>()) } just runs
            }
        }

        every { patcher.context } returns patcherContext
    }

    @Test
    fun `executes patches in correct order`() {
        val executed = mutableListOf<String>()

        val patches = setOf(
            bytecodePatch { execute { executed += "1" } },
            bytecodePatch {
                dependsOn(
                    bytecodePatch {
                        execute { executed += "2" }
                        finalize { executed += "-2" }
                    },
                    bytecodePatch { execute { executed += "3" } },
                )

                execute { executed += "4" }
                finalize { executed += "-1" }
            },
        )

        assert(executed.isEmpty())

        patches()

        assertEquals(
            listOf("1", "2", "3", "4", "-1", "-2"),
            executed,
            "Expected patches to be executed in correct order.",
        )
    }

    @Test
    fun `handles execution of patches correctly when exceptions occur`() {
        val executed = mutableListOf<String>()

        infix fun Patch<*>.produces(equals: List<String>) {
            val patches = setOf(this)

            try {
                patches()
            } catch (_: PatchException) {
                // Swallow expected exceptions for testing purposes.
            }

            assertEquals(equals, executed, "Expected patches to be executed in correct order.")

            executed.clear()
        }

        // No patches execute successfully,
        // because the dependency patch throws an exception inside the execute block.
        bytecodePatch {
            dependsOn(
                bytecodePatch {
                    execute { throw PatchException("1") }
                    finalize { executed += "-2" }
                },
            )

            execute { executed += "2" }
            finalize { executed += "-1" }
        } produces emptyList()

        // The dependency patch is executed successfully,
        // because only the dependant patch throws an exception inside the finalize block.
        // Patches that depend on a failed patch should not be executed,
        // but patches that are depended on by a failed patch should be executed.
        bytecodePatch {
            dependsOn(
                bytecodePatch {
                    execute { executed += "1" }
                    finalize { executed += "-2" }
                },
            )

            execute { throw PatchException("2") }
            finalize { executed += "-1" }
        } produces listOf("1", "-2")

        // Because the finalize block of the dependency patch is executed after the finalize block of the dependant patch,
        // the dependant patch executes successfully, but the dependency patch raises an exception in the finalize block.
        bytecodePatch {
            dependsOn(
                bytecodePatch {
                    execute { executed += "1" }
                    finalize { throw PatchException("-2") }
                },
            )

            execute { executed += "2" }
            finalize { executed += "-1" }
        } produces listOf("1", "2", "-1")

        // The dependency patch is executed successfully,
        // because the dependant patch raises an exception in the finalize block.
        // Patches that depend on a failed patch should not be executed,
        // but patches that are depended on by a failed patch should be executed.
        bytecodePatch {
            dependsOn(
                bytecodePatch {
                    execute { executed += "1" }
                    finalize { executed += "-2" }
                },
            )

            execute { executed += "2" }
            finalize { throw PatchException("-1") }
        } produces listOf("1", "2", "-2")
    }

    @Test
    fun `throws if unmatched fingerprint match is used`() {
        val patch = bytecodePatch {
            execute {
                // Fingerprint can never match.
                val fingerprint = fingerprint {
                    strings("doesnt exist")
                }

                // Throws, because the fingerprint can't be matched.
                fingerprint.patternMatch
            }
        }

        assertThrows<PatchException>("Expected an exception because the fingerprint can't match.") { patch() }
    }

    @Test
    fun `matcher finds indices correctly`() {
        val iterable = (1..10).toList()
        val matcher = indexedMatcher<Int>()

        matcher.apply {
            +head { this > 5 }
        }
        assertFalse(
            matcher(iterable),
            "Should not match at any other index than first"
        )
        matcher.clear()

        matcher.apply { +head { this == 1 } }(iterable)
        assertEquals(
            listOf(0),
            matcher.indices,
            "Should match at first index."
        )
        matcher.clear()

        matcher.apply { add { _, _ -> this > 0 } }(iterable)
        assertEquals(1, matcher.indices.size, "Should only match once.")
        matcher.clear()

        matcher.apply { add { _, _ -> this == 2 } }(iterable)
        assertEquals(
            listOf(1),
            matcher.indices,
            "Should find the index correctly."
        )
        matcher.clear()

        matcher.apply {
            +head { this == 1 }
            add { _, _ -> this == 2 }
            add { _, _ -> this == 4 }
        }(iterable)
        assertEquals(
            listOf(0, 1, 3),
            matcher.indices,
            "Should match 1, 2 and 4 at indices 0, 1 and 3."
        )
        matcher.clear()

        matcher.apply {
            +after { this == 1 }
        }(iterable)
        assertEquals(
            listOf(0),
            matcher.indices,
            "Should match index 0 after nothing"
        )
        matcher.clear()

        matcher.apply {
            +after(2..Int.MAX_VALUE) { this == 1 }
        }
        assertFalse(
            matcher(iterable),
            "Should not match, because 1 is out of range"
        )
        matcher.clear()

        matcher.apply {
            +after(1..1) { this == 2 }
        }
        assertFalse(
            matcher(iterable),
            "Should not match, because 2 is at index 1"
        )
        matcher.clear()

        matcher.apply {
            +head { this == 1 }
            +after(2..5) { this == 4 }
            add { _, _ -> this == 8 }
            add { _, _ -> this == 9 }
        }(iterable)
        assertEquals(
            listOf(0, 3, 7, 8),
            matcher.indices,
            "Should match indices correctly."
        )
    }

    @Test
    fun `matches via composite`() {
        fun composite(fail: Boolean = false) = firstMethodComposite {
            name("method")
            definingClass("class")

            if (fail) returnType("doesnt exist")

            instructions(
                head(Opcode.CONST_STRING()),
                `is`<TwoRegisterInstruction>(),
                noneOf(registers()),
                string("test", String::contains),
                after(1..3, allOf(Opcode.INVOKE_VIRTUAL(), registers(1, 0))),
                allOf(),
                type("PrintStream;", String::endsWith)
            )
        }

        with(patcher.context.bytecodeContext) {
            assertNotNull(composite().methodOrNull) {
                "Expected to find a method matching the composite fingerprint."
            }
            assertNull(composite(fail = true).methodOrNull) {
                "Expected to not find a method matching the composite fingerprint."
            }
        }
    }

    @Test
    fun `matches fingerprint`() {
        val fingerprint = fingerprint { returns("V") }
        val fingerprint2 = fingerprint { returns("V") }
        val fingerprint3 = fingerprint { returns("V") }

        with(patcher.context.bytecodeContext) {
            assertAll(
                "Expected fingerprints to match.",
                { assertNotNull(fingerprint.matchOrNull(this.classDefs.first().methods.first())) },
                { assertNotNull(fingerprint2.matchOrNull(this.classDefs.first())) },
                { assertNotNull(fingerprint3.originalClassDefOrNull) },
            )
        }
    }

    private operator fun Set<Patch<*>>.invoke(): List<PatchResult> {
        // TODO: Ideally most of this mocking could be moved to setUp,
        //  but by doing so the mocking breaks and it is not clear why.

        every { patcherContext.executablePatches } returns toMutableSet()

        return runBlocking {
            patcher().toList().also { results ->
                results.firstOrNull { result -> result.exception != null }?.let { result -> throw result.exception!! }
            }
        }
    }

    private operator fun Patch<*>.invoke() = setOf(this)().first()

    private fun Any.setPrivateField(field: String, value: Any) {
        this::class.java.getDeclaredField(field).apply {
            this.isAccessible = true
            set(this@setPrivateField, value)
        }
    }
}
