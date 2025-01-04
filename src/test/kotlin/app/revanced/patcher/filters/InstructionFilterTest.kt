package app.revanced.patcher.filters

import app.revanced.patcher.MethodFilter
import org.junit.jupiter.api.assertAll
import org.junit.jupiter.api.assertThrows
import kotlin.test.Test
import kotlin.test.assertTrue

internal object InstructionFilterTest {
    @Test
    fun `MethodFilter toString parsing`() {
        var definingClass = "Landroid/view/View;"
        var methodName = "inflate"
        var parameters = listOf("[Ljava/lang/String;", "I", "Z", "F", "J", "Landroid/view/ViewGroup;")
        var returnType = "Landroid/view/View;"
        var methodSignature = "$definingClass->$methodName(${parameters.joinToString("")})$returnType"

        var filter = MethodFilter.parseJvmMethodCall(methodSignature)

        assertAll(
            "toStringParsing matches",
            { assertTrue(definingClass == filter.definingClass!!()) },
            { assertTrue(methodName == filter.methodName!!()) },
            { assertTrue(parameters == filter.parameters!!()) },
            { assertTrue(returnType == filter.returnType!!()) },
        )


        definingClass = "Landroid/view/View;"
        methodName = "inflate"
        parameters = listOf("[Ljava/lang/String;", "I", "Z", "F", "J", "Landroid/view/ViewGroup;")
        returnType = "V"
        methodSignature = "$definingClass->$methodName(${parameters.joinToString("")})$returnType"

        filter = MethodFilter.parseJvmMethodCall(methodSignature)

        assertAll(
            "toStringParsing matches",
            { assertTrue(definingClass == filter.definingClass!!()) },
            { assertTrue(methodName == filter.methodName!!()) },
            { assertTrue(parameters == filter.parameters!!()) },
            { assertTrue(returnType == filter.returnType!!()) },
        )

    }

    @Test
    fun `MethodFilter toString bad input`() {
        var definingClass = "Landroid/view/View" // Missing semicolon
        var methodName = "inflate"
        var parameters = listOf("[Ljava/lang/String;", "I", "Z", "F", "J", "Landroid/view/ViewGroup;")
        var returnType = "Landroid/view/View;"
        var methodSignature = "$definingClass->$methodName(${parameters.joinToString("")})$returnType"

        assertThrows<IllegalArgumentException> {
            MethodFilter.parseJvmMethodCall(methodSignature)
        }


        definingClass = "Landroid/view/View;"
        methodName = "inflate"
        // Missing semicolon
        parameters = listOf("[Ljava/lang/String;", "I", "Z", "F", "J", "Landroid/view/ViewGroup")
        returnType = "Landroid/view/View;"
        methodSignature = "$definingClass->$methodName(${parameters.joinToString("")})$returnType"

        assertThrows<IllegalArgumentException> {
            MethodFilter.parseJvmMethodCall(methodSignature)
        }


        definingClass = "Landroid/view/View;"
        methodName = "inflate"
        parameters = listOf("[Ljava/lang/String;", "I", "Z", "F", "J", "Landroid/view/ViewGroup;")
        returnType = "" // Missing return type
        methodSignature = "$definingClass->$methodName(${parameters.joinToString("")})$returnType"

        assertThrows<IllegalArgumentException> {
            MethodFilter.parseJvmMethodCall(methodSignature)
        }
    }
}
