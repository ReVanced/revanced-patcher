package app.revanced.patcher.writer

import org.objectweb.asm.tree.AbstractInsnNode
import org.objectweb.asm.tree.InsnList

object ASMWriter {
    fun InsnList.setAt(index: Int, node: AbstractInsnNode) {
        this[this.get(index)] = node
    }

    fun InsnList.insertAt(index: Int = 0, vararg nodes: AbstractInsnNode) {
        this.insert(this.get(index), nodes.toInsnList())
    }

    // TODO(Sculas): Should this be public?
    private fun Array<out AbstractInsnNode>.toInsnList(): InsnList {
        val list = InsnList()
        this.forEach { list.add(it) }
        return list
    }
}