package app.revanced.patcher.util

import app.revanced.patcher.patch.PatchException
import app.revanced.patcher.util.proxy.mutableTypes.MutableClass
import com.android.tools.smali.dexlib2.iface.ClassDef
import kotlin.collections.mutableMapOf

@Deprecated("Instead use PatchClasses")
typealias ProxyClassList = PatchClasses

/**
 * A set of all classes for the target app and any extension classes.
 */
class PatchClasses internal constructor(
    /**
     * Pool of both immutable and mutable classes.
     */
    internal val pool: MutableMap<String, ClassDef>
) {
    /**
     * Mutable classes. All instances are also found in [pool].
     */
    private val mutablePool = mutableMapOf<String, MutableClass>()

    internal fun addClass(classDef: ClassDef) {
        pool[classDef.type] = classDef
    }

    internal fun close() {
        pool.clear()
        mutablePool.clear()
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
    fun mutableClassBy(classDefType: String) =
        mutablePool[classDefType] ?: MutableClass(
            pool.get(classDefType) ?: throw PatchException("Could not find class: $classDefType")
        ).also {
            mutablePool[classDefType] = it
            pool[classDefType] = it
        }

    /**
     * Find a class with a predicate.
     *
     * @param classDef An immutable class.
     * @return A mutable version of the class definition.
     */
    fun mutableClassBy(classDef: ClassDef) =
        mutablePool[classDef.type] ?: MutableClass(classDef).also {
            val classType = classDef.type
            mutablePool[classType] = it
            pool[classType] = it
        }

    /**
     * Find a class with a predicate.
     *
     * @param predicate A predicate to match the class.
     * @return A mutable class that matches the predicate.
     */
    fun mutableClassBy(predicate: (ClassDef) -> Boolean) =
        mutablePool.values.find { predicate(it) } ?: pool.values.find(predicate)?.let { mutableClassBy(it) }
}
