package net.revanced.patcher.signature.model

import org.objectweb.asm.Type
import org.objectweb.asm.tree.ParameterNode

/**
 * An ASM signature list for the Patcher.
 *
 * @param name The name of the method.
 * Do not use the actual method name, instead try to guess what the method name originally was.
 * If you are unable to guess a method name, doing something like "patch-name-1" is fine too.
 * For example: "override-codec-1".
 * This method name will be used to find the corresponding patch.
 * @param returns The return type/signature of the method.
 * @param accessors The accessors of the method.
 * @param parameters The parameter types/signatures of the method.
 * @param opcodes The opcode pattern of the method, used to find the method by signature scanning.
 */
data class Signature(
    val name: String,
    val returns: Type,
    @Suppress("ArrayInDataClass") val accessors: Int,
    @Suppress("ArrayInDataClass") val parameters: Array<ParameterNode>,
    @Suppress("ArrayInDataClass") val opcodes: Array<Int>
)