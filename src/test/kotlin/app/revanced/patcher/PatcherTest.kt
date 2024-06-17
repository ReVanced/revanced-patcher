package app.revanced.patcher

import app.revanced.patcher.patch.Patch
import app.revanced.patcher.patch.PatchResult
import app.revanced.patcher.patch.ResourcePatchContext
import app.revanced.patcher.patch.bytecodePatch
import app.revanced.patcher.util.ProxyClassList
import com.android.tools.smali.dexlib2.immutable.ImmutableClassDef
import com.android.tools.smali.dexlib2.immutable.ImmutableMethod
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.assertDoesNotThrow
import java.util.logging.Logger
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

internal object PatcherTest {
    private lateinit var patcher: Patcher

    @BeforeEach
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

            every { context.bytecodeContext.classes } returns mockk(relaxed = true)
            every { context.bytecodeContext.integrations } returns mockk(relaxed = true)
            every { apply(false) } answers { callOriginal() }
        }
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
    fun `throws if unmatched fingerprint match is delegated`() {
        val patch = bytecodePatch {
            // Fingerprint can never match.
            val match by fingerprint { }
            // Manually add the fingerprint.
            app.revanced.patcher.fingerprint { }()

            execute {
                // Throws, because the fingerprint can't be matched.
                match.patternMatch
            }
        }

        assertEquals(2, patch.fingerprints.size)

        assertTrue(
            patch().exception != null,
            "Expected an exception because the fingerprint can't match.",
        )
    }

    @Test
    fun `matches fingerprint`() {
        mockClassWithMethod()

        val patches = setOf(bytecodePatch { fingerprint { this returns "V" }() })

        assertNull(
            patches.first().fingerprints.first().match,
            "Expected fingerprint to be matched before execution.",
        )

        patches()

        assertDoesNotThrow("Expected fingerprint to be matched.") {
            assertEquals(
                "V",
                patches.first().fingerprints.first().match!!.method.returnType,
                "Expected fingerprint to be matched.",
            )
        }
    }
    private operator fun Set<Patch<*>>.invoke(): List<PatchResult> {
        every { patcher.context.executablePatches } returns toMutableSet()

        return runBlocking { patcher.apply(false).toList() }
    }

    private operator fun Patch<*>.invoke() = setOf(this)().first()

    private fun Any.setPrivateField(field: String, value: Any) {
        this::class.java.getDeclaredField(field).apply {
            this.isAccessible = true
            set(this@setPrivateField, value)
        }
    }

    private fun mockClassWithMethod() {
        every { patcher.context.bytecodeContext.classes } returns ProxyClassList(
            mutableListOf(
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
                            null,
                        ),
                    ),
                ),
            ),
        )
    }
}
