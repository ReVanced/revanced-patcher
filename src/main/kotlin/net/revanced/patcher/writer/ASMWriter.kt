package net.revanced.patcher.writer

import org.objectweb.asm.tree.AbstractInsnNode
import org.objectweb.asm.tree.InsnList

object ASMWriter {
    fun InsnList.setAt(index: Int, node: AbstractInsnNode) {
        this[this.get(index)] = node
    }
}