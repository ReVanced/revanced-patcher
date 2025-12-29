package app.revanced.patcher

import app.revanced.patcher.patch.bytecodePatch
import com.android.tools.smali.dexlib2.Opcode
import com.android.tools.smali.dexlib2.iface.instruction.TwoRegisterInstruction
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.api.assertDoesNotThrow
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
object MatchingTest : PatcherTestBase() {
    @BeforeAll
    fun setUp() = setUpMock()

    @Test
    fun `matches via builder api`() {
        fun firstMethodBuilder(fail: Boolean = false) = firstMethodBuilder {
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

        bytecodePatch {
            execute {
                assertNotNull(firstMethodBuilder().methodOrNull) { "Expected to find a method" }
                Assertions.assertNull(firstMethodBuilder(fail = true).methodOrNull) { "Expected to not find a method" }
            }
        }()
    }

    @Test
    fun `matches via declarative api`() {
        bytecodePatch {
            execute {
                val method = firstMethodByDeclarativePredicateOrNull {
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
    fun `predicate matcher works correctly`() {
        bytecodePatch {
            execute {
                assertDoesNotThrow("Should find method") { firstMethod { name == "method" } }
            }
        }
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
}