package app.revanced.patcher.util

import app.revanced.patcher.BytecodeContext
import app.revanced.patcher.util.proxy.mutableTypes.MutableClass

object TypeUtil {
    /**
     * Traverse the class hierarchy starting from the given root class.
     *
     * @param targetClass The class to start traversing the class hierarchy from.
     * @param callback The function that is called for every class in the hierarchy.
     */
    fun BytecodeContext.traverseClassHierarchy(targetClass: MutableClass, callback: MutableClass.() -> Unit) {
        callback(targetClass)
        this.classes.findClassProxied(targetClass.superclass ?: return)?.mutableClass?.let {
            traverseClassHierarchy(it, callback)
        }
    }
}
