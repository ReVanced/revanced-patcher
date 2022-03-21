package app.revanced.patcher.util

import org.objectweb.asm.tree.AbstractInsnNode
import org.objectweb.asm.tree.FieldInsnNode
import org.objectweb.asm.tree.LdcInsnNode
import kotlin.test.fail

object TestUtil {
    fun <T: AbstractInsnNode> assertNodeEqual(expected: T, actual: T) {
        val a = expected.nodeString()
        val b = actual.nodeString()
        if (a != b) {
            fail("expected: $a,\nactual: $b\n")
        }
    }

    private fun AbstractInsnNode.nodeString(): String {
        val sb = NodeStringBuilder()
        when (this) {
            // TODO(Sculas): Add more types
            is LdcInsnNode -> sb
                .addType("cst", cst)
            is FieldInsnNode -> sb
                .addType("owner", owner)
                .addType("name", name)
                .addType("desc", desc)
        }
        return "(${this::class.simpleName}): (type = $type, opcode = $opcode, $sb)"
    }
}

private class NodeStringBuilder {
    private val sb = StringBuilder()

    fun addType(name: String, value: Any): NodeStringBuilder {
        sb.append("$name = \"$value\", ")
        return this
    }

    override fun toString(): String {
        val s = sb.toString()
        return s.substring(0 until s.length - 2) // remove the last ", "
    }
}
