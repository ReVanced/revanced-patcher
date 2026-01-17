package app.revanced.patcher

import app.revanced.patcher.BytecodePatchContextMethodMatching.firstMethod
import app.revanced.patcher.BytecodePatchContextMethodMatching.firstMethodDeclarativelyOrNull
import app.revanced.patcher.InstructionMatchingFunctions.invoke
import app.revanced.patcher.InstructionMatchingFunctions.`is`
import app.revanced.patcher.InstructionMatchingFunctions.registers
import app.revanced.patcher.InstructionMatchingFunctions.string
import app.revanced.patcher.InstructionMatchingFunctions.type
import app.revanced.patcher.patch.bytecodePatch
import com.android.tools.smali.dexlib2.Opcode
import com.android.tools.smali.dexlib2.iface.instruction.TwoRegisterInstruction
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.api.assertDoesNotThrow
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class MatchingTest : PatcherTestBase() {
    @BeforeAll
    fun setup() = setupMock()

    @Test
    fun `finds via builder api`() {
        fun firstMethodComposite(fail: Boolean = false) = firstMethodComposite {
            name("method")
            definingClass("class")

            if (fail) returnType("doesnt exist")

            strings("This is a test.")
            strings(StringMatchingFunctions.string("Hello", String::startsWith))

            instructions(
                at(Opcode.CONST_STRING()),
                `is`<TwoRegisterInstruction>(),
                noneOf(registers()),
                string("test", String::contains),
                after(1..3, allOf(Opcode.INVOKE_VIRTUAL(), registers(1, 0))),
                allOf(),
                type("PrintStream;", String::endsWith)
            )
        }

        with(bytecodePatchContext) {
            val match = firstMethodComposite()
            assertNotNull(
                match.methodOrNull,
                "Expected to find a method"
            )
            assertEquals(
                4, match.indices[3],
                "Expected to find the string instruction at index 5"
            )
            assertEquals(
                0, match.stringIndices["Hello"],
                "Expected to find 'Hello' at index 0"
            )

            assertNull(
                firstMethodComposite(fail = true).immutableMethodOrNull,
                "Expected to not find a method"
            )

            assertNotNull(
                firstMethodComposite().match(classDefs.first()).methodOrNull,
                "Expected to find a method matching in a specific class"
            )
        }
    }

    @Test
    fun `finds via declarative api`() {
        bytecodePatch {
            apply {
                val method = firstMethodDeclarativelyOrNull {
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
    fun `unordered matcher works correctly`() {
        val strings = listOf("apple", "banana", "cherry", "date", "elderberry")
        val matcher = unorderedMatcher<String, String>()

        matcher.apply {
            add { "an".takeIf { contains(it) } }
            add { "apple".takeIf { equals(it) } }
            add { "elder".takeIf { startsWith(it) } }
        }
        assertTrue(
            matcher(strings),
            "Should match correctly"
        )
        assertEquals(
            matcher.indices["an"], 1,
            "Should find 'banana' at index 1"
        )
        assertEquals(
            matcher.indices["apple"], 0,
            "Should find 'apple' at index 0"
        )
        assertEquals(
            matcher.indices["elder"], 4,
            "Should find 'elderberry' at index 4"
        )
        matcher.clear()

        matcher.apply {
            add { "xyz".takeIf { contains(it) } }
            add { "apple".takeIf { equals(it) } }
            add { "elder".takeIf { startsWith(it) } }
        }
        assertFalse(
            matcher(strings),
            "Should not match"
        )
    }

    @Test
    fun `indexed matcher finds indices correctly`() {
        val iterable = (1..10).toList()
        val matcher = indexedMatcher<Int>()

        matcher.apply {
            +at<Int> { this > 5 }
        }
        assertFalse(
            matcher(iterable),
            "Should not match at any other index than first"
        )
        matcher.clear()

        matcher.apply { +at<Int> { this == 1 } }(iterable)
        assertEquals(
            listOf(0),
            matcher.indices,
            "Should match at first index."
        )
        matcher.clear()

        matcher.apply { add { _, _, _ -> this > 0 } }(iterable)
        assertEquals(1, matcher.indices.size, "Should only match once.")
        matcher.clear()

        matcher.apply { add { _, _, _ -> this == 2 } }(iterable)
        assertEquals(
            listOf(1),
            matcher.indices,
            "Should find the index correctly."
        )
        matcher.clear()

        matcher.apply {
            +at<Int> { this == 1 }
            add { _, _, _ -> this == 2 }
            add { _, _, _ -> this == 4 }
        }(iterable)
        assertEquals(
            listOf(0, 1, 3),
            matcher.indices,
            "Should match 1, 2 and 4 at indices 0, 1 and 3."
        )
        matcher.clear()

        matcher.apply {
            +after<Int> { this == 1 }
        }(iterable)
        assertEquals(
            listOf(0),
            matcher.indices,
            "Should match index 0 after nothing"
        )
        matcher.clear()

        matcher.apply {
            +after<Int>(2..Int.MAX_VALUE) { this == 1 }
        }
        assertFalse(
            matcher(iterable),
            "Should not match, because 1 is out of range"
        )
        matcher.clear()

        matcher.apply {
            +after<Int>(1..1) { this == 2 }
        }
        assertFalse(
            matcher(iterable),
            "Should not match, because 2 is at index 1"
        )
        matcher.clear()

        matcher.apply {
            +at<Int> { this == 1 }
            +after<Int>(2..5) { this == 4 }
            add { _, _, _ -> this == 8 }
            add { _, _, _ -> this == 9 }
        }(iterable)
        assertEquals(
            listOf(0, 3, 7, 8),
            matcher.indices,
            "Should match indices correctly."
        )
    }
}