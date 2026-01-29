package app.revanced.patcher

import app.revanced.patcher.extensions.instructions
import app.revanced.patcher.patch.bytecodePatch
import com.android.tools.smali.dexlib2.Opcode
import com.android.tools.smali.dexlib2.iface.instruction.Instruction
import com.android.tools.smali.dexlib2.iface.instruction.TwoRegisterInstruction
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.api.assertDoesNotThrow
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class MatchingTest : PatcherTestBase() {
    @BeforeAll
    fun setup() = setupMock()

    @Test
    fun `finds via composite api`() {
        fun build(fail: Boolean = false): DeclarativePredicateCompositeBuilder =
            {
                name("method")
                definingClass("class")

                if (fail) returnType("doesnt exist")

                instructions(
                    at(0, Opcode.CONST_STRING()),
                    `is`<TwoRegisterInstruction>(),
                    noneOf(registers()),
                    string("test", String::contains),
                    after(1..3, allOf(Opcode.INVOKE_VIRTUAL(), registers(1, 0))),
                    allOf(),
                    type("PrintStream;", String::endsWith),
                )
            }

        with(bytecodePatchContext) {
            val match = firstMethodComposite(build = build())
            assertNotNull(
                match.methodOrNull,
                "Expected to find a method",
            )
            assertEquals(
                4,
                match[3],
                "Expected to find the string instruction at index 4",
            )

            assertNull(
                firstMethodComposite(build = build(fail = true)).immutableMethodOrNull,
                "Expected to not find a method",
            )

            assertNotNull(
                classDefs.first().firstMethodComposite(build = build()).methodOrNull,
                "Expected to find a method matching in a specific class",
            )
        }
    }

    @Test
    fun `finds via declarative api`() {
        bytecodePatch {
            apply {
                val method =
                    firstMethodDeclarativelyOrNull {
                        anyOf {
                            predicate { name == "method" }
                            add { false }
                        }
                        allOf {
                            predicate { returnType == "V" }
                        }
                        predicate { definingClass == "class" }
                    }
                assertNotNull(method) { "Expected to find a method" }
            }
        }()
    }

    @Test
    fun `predicate api works correctly`() {
        bytecodePatch {
            apply {
                assertDoesNotThrow("Should find method") { firstMethod { name == "method" } }
            }
        }
    }

    @Test
    fun `indexed matcher finds indices correctly`() {
        val iterable = (1..10).toList()
        val matcher = indexedMatcher<Int>()

        matcher.apply {
            +at<Int>(0) { this > 5 }
        }
        assertFalse(
            matcher(iterable),
            "Should not match at any other index than first",
        )
        matcher.clear()

        matcher.apply { +at<Int>(0) { this == 1 } }(iterable)
        assertEquals(
            listOf(0),
            matcher.indices,
            "Should match at first index.",
        )
        matcher.clear()

        matcher.apply { add { _, _, _ -> this > 0 } }(iterable)
        assertEquals(1, matcher.indices.size, "Should only match once.")
        matcher.clear()

        matcher.apply { add { _, _, _ -> this == 2 } }(iterable)
        assertEquals(
            listOf(1),
            matcher.indices,
            "Should find the index correctly.",
        )
        matcher.clear()

        matcher.apply {
            +at<Int>(0) { this == 1 }
            add { _, _, _ -> this == 2 }
            add { _, _, _ -> this == 4 }
        }(iterable)
        assertEquals(
            listOf(0, 1, 3),
            matcher.indices,
            "Should match 1, 2 and 4 at indices 0, 1 and 3.",
        )
        matcher.clear()

        matcher.apply {
            +after<Int> { this == 1 }
        }(iterable)
        assertEquals(
            listOf(0),
            matcher.indices,
            "Should match index 0 after nothing",
        )
        matcher.clear()

        matcher.apply {
            +after<Int>(2..Int.MAX_VALUE) { this == 1 }
        }
        assertFalse(
            matcher(iterable),
            "Should not match, because 1 is out of range",
        )
        matcher.clear()

        matcher.apply {
            +after<Int>(1..1) { this == 2 }
        }
        assertFalse(
            matcher(iterable),
            "Should not match, because 2 is at index 1",
        )
        matcher.clear()

        matcher.apply {
            +at<Int>(0) { this == 1 }
            +after<Int>(2..5) { this == 4 }
            add { _, _, _ -> this == 8 }
            add { _, _, _ -> this == 9 }
        }(iterable)
        assertEquals(
            listOf(0, 3, 7, 8),
            matcher.indices,
            "Should match indices correctly.",
        )
    }

    @Test
    fun `unordered matching works correctly`() {
        val list =
            bytecodePatchContext.classDefs
                .first()
                .methods
                .first()
                .instructions
        val matcher = indexedMatcher<Instruction>()

        matcher.apply {
            addAll(
                unorderedAllOf(
                    afterAtLeast(1, Opcode.RETURN_OBJECT()),
                    string(),
                    Opcode.INVOKE_VIRTUAL(),
                ),
            )
        }(list)
        assertEquals(
            listOf(4, 5, 6),
            matcher.indices,
            "Should match because after(1) luckily only matches after the string at index 4.",
        )
        matcher.clear()

        matcher.apply {
            addAll(
                unorderedAllOf(
                    string("test", String::contains),
                    string("Hello", String::contains),
                    afterAtLeast(1, Opcode.RETURN_OBJECT()),
                ),
            )
        }(list)
        assertEquals(
            listOf(0, 4, 5),
            matcher.indices,
            "Should first match indices 4 due to the string, then after due to step 1, then the invoke.",
        )

        assertFalse(
            indexedMatcher<Int>(
                items =
                    unorderedAllOf(
                        { _, _, _ -> this == 1 },
                        { _, _, _ -> this == -1 },
                        after(2) { this == -2 },
                    ),
            )(listOf(1, -1, 1, 2, -2)),
            "Should not match because because 1 is matched at index 0, too early for after(2).",
        )
    }
}
