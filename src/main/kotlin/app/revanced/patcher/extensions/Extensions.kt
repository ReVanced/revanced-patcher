package app.revanced.patcher.extensions

import app.revanced.patcher.util.proxy.mutableTypes.MutableMethod

/**
 * Create a label for the instruction at given index.
 *
 * @param index The index to create the label for the instruction at.
 * @return The label.
 */
fun MutableMethod.newLabel(index: Int) = implementation!!.newLabelForIndex(index)
