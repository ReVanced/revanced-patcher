package app.revanced.patcher.util

import app.revanced.patcher.patch.PatchException
import app.revanced.patcher.util.proxy.mutableTypes.MutableClass
import com.android.tools.smali.dexlib2.iface.ClassDef

@Deprecated("Instead use PatchClasses")
typealias ProxyClassList = PatchClasses

/**
 * All classes for the target app and any extension classes.
 */
class PatchClasses internal constructor(
    /**
     * Pool of both immutable and mutable classes.
     */
    internal val pool: MutableMap<String, ClassDef>
) {

    internal constructor(set: Set<ClassDef>) :
            this(set.associateByTo(mutableMapOf()) { it.type })

    internal fun addClass(classDef: ClassDef) {
        pool[classDef.type] = classDef
    }

    internal fun close() {
        pool.clear()
    }

    /**
     * Iterate over all classes.
     */
    fun forEach(action: (ClassDef) -> Unit) {
        pool.values.forEach(action)
    }

    /**
     * Find a class with a predicate.
     *
     * @param classType The full classname.
     * @return An immutable instance of the class type.
     * @see mutableClassBy
     */
    fun classBy(classType: String) = pool[classType]

    /**
     * Find a class with a predicate.
     *
     * @param predicate A predicate to match the class.
     * @return An immutable instance of the class type.
     */
    fun classBy(predicate: (ClassDef) -> Boolean) = pool.values.find(predicate)

    /**
     * Find a class with a predicate.
     *
     * @param classDefType The full classname.
     * @return A mutable version of the class type.
     */
    fun mutableClassBy(classDefType: String) : MutableClass {
        var classDef = pool[classDefType] ?: throw PatchException("Could not find class: $classDefType")
        if (classDef is MutableClass) {
            return classDef
        }
        classDef = MutableClass(classDef)
        pool[classDefType] = classDef
        return classDef
    }

    /**
     * @param classDef An immutable class.
     * @return A mutable version of the class definition.
     */
    fun mutableClassBy(classDef: ClassDef) =
        if (classDef is MutableClass) classDef else mutableClassBy(classDef.type)

    /**
     * Find a mutable class with a predicate.
     *
     * @param predicate A predicate to match the class.
     * @return A mutable class that matches the predicate.
     */
    fun mutableClassBy(predicate: (ClassDef) -> Boolean) =
        classBy(predicate)?.let {
            if (it is MutableClass) it else mutableClassBy(it.type)
        }
}
