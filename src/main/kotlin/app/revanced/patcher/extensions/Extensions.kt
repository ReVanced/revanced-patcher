package app.revanced.patcher.extensions

import app.revanced.patcher.util.proxy.mutableTypes.MutableMethod
import org.jf.dexlib2.AccessFlags

/**
 * Create a label for the instruction at given index.
 *
 * @param index The index to create the label for the instruction at.
 * @return The label.
 */
fun MutableMethod.newLabel(index: Int) = implementation!!.newLabelForIndex(index)

/**
 * Perform a bitwise OR operation between two [AccessFlags].
 *
 * @param other The other [AccessFlags] to perform the operation with.
 */
infix fun AccessFlags.or(other: AccessFlags) = value or other.value

/**
 * Perform a bitwise OR operation between an [AccessFlags] and an [Int].
 *
 * @param other The [Int] to perform the operation with.
 */
infix fun Int.or(other: AccessFlags) = this or other.value

/**
 * Perform a bitwise OR operation between an [Int] and an [AccessFlags].
 *
 * @param other The [AccessFlags] to perform the operation with.
 */
infix fun AccessFlags.or(other: Int) = value or other