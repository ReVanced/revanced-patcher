package app.revanced.patcher.signature

import org.objectweb.asm.Type

/**
 * An ASM signature list for the Patcher.
 *
 * @param name The name of the method.
 * Do not use the actual method name, instead try to guess what the method name originally was.
 * If you are unable to guess a method name, doing something like "patch-name-1" is fine too.
 * For example: "override-codec-1".
 * This method name will be mapped to the method matching the signature.
 * Even though this is technically not needed for the `findParentMethod` method,
 * it is still recommended giving the method a name, so it can be identified easily.
 * @param returns The return type/signature of the method.
 * @param accessors The accessors of the method.
 * @param parameters The parameter types of the method.
 * @param opcodes The opcode pattern of the method, used to find the method by pattern scanning.
 */
@Suppress("ArrayInDataClass")
data class Signature(
    val name: String,
    val returns: Type?,
    val accessors: Int?,
    val parameters: Array<Type>?,
    val opcodes: Array<Int>?
)