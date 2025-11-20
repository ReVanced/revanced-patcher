package app.revanced.patcher.util

import app.revanced.patcher.extensions.InstructionExtensions.instructionsOrNull
import app.revanced.patcher.patch.PatchException
import app.revanced.patcher.util.proxy.mutableTypes.MutableClass
import com.android.tools.smali.dexlib2.Opcode
import com.android.tools.smali.dexlib2.iface.ClassDef
import com.android.tools.smali.dexlib2.iface.instruction.ReferenceInstruction
import com.android.tools.smali.dexlib2.iface.reference.StringReference
import java.util.LinkedList

@Deprecated("Instead use PatchClasses")
typealias ProxyClassList = PatchClasses

/**
 * All classes for the target app and any extension classes.
 */
class PatchClasses internal constructor(
    /**
     * Map of both immutable and mutable classes and the strings contained.
     */
    internal val classMap: MutableMap<String, PatchesClassMapValue>
) {

    /**
     * Container to hold both the class definition and all strings found in the class.
     * This intermediate class is used to remove the need of updating the string map
     * when a class is upgraded to mutable.
     */
    internal class PatchesClassMapValue(
        /**
         * Can be immutable or mutable.
         */
        var classDef: ClassDef,
    ) {
        /**
         * Strings found in all methods of the class.
         */
        val strings by lazy {
            var list : LinkedList<String>? = null

            classDef.methods.forEach { method ->
                // Add strings contained in the method as the key.
                method.instructionsOrNull?.forEach { instruction ->
                    val opcode = instruction.opcode
                    if (opcode != Opcode.CONST_STRING && opcode != Opcode.CONST_STRING_JUMBO) {
                        return@forEach
                    }

                    val string = ((instruction as ReferenceInstruction).reference as StringReference).string

                    if (list == null) {
                        list = LinkedList()
                    }
                    list.add(string)
                }
            }

            list
        }

        fun getMutableClass(): MutableClass {
            if (classDef !is MutableClass) {
                classDef = MutableClass(classDef)
            }
            return classDef as MutableClass
        }
    }

    /**
     * Methods associated by strings referenced in them.
     * Effectively this is a reverse lookup map to values in the [classMap].
     */
    private var _stringMap: HashMap<String, LinkedList<PatchesClassMapValue>>? = null

    internal constructor(set: Set<ClassDef>) : this(set.map {
        PatchesClassMapValue(it)
    }.associateByTo(HashMap(set.size * 3 / 2)) { classDefStrings ->
        classDefStrings.classDef.type
    })

    internal fun close() {
        classMap.clear()
        closeStringMap()
    }

    internal fun closeStringMap() {
        _stringMap = null
    }

    internal fun addClass(classDef: ClassDef) {
        classMap[classDef.type] = PatchesClassMapValue(classDef)
    }

    /**
     * Iterate over all classes.
     */
    fun forEach(action: (ClassDef) -> Unit) {
        classMap.values.forEach { poolValue ->
            action(poolValue.classDef)
        }
    }

    /**
     * Find a class with a predicate.
     *
     * @param classType The full classname.
     * @return An immutable instance of the class type.
     * @see mutableClassBy
     */
    fun classByOrNull(classType: String) = classMap[classType]?.classDef

    /**
     * Find a class with a predicate. If you know the class type name,
     * it is highly preferred to instead use [classByOrNull(String)].
     *
     * @param predicate A predicate to match the class.
     * @return An immutable instance of the class type, or null if not found.
     */
    fun classByOrNull(predicate: (ClassDef) -> Boolean) = poolValueByOrNull(predicate)?.classDef

    private fun poolValueByOrNull(predicate: (ClassDef) -> Boolean) =
        classMap.values.find { poolValue ->
            predicate(poolValue.classDef)
        }

    /**
     * Find a class with a predicate.
     *
     * @param predicate A predicate to match the class.
     * @return An immutable instance of the class type.
     */
    fun classBy(predicate: (ClassDef) -> Boolean) = classByOrNull(predicate)
        ?: throw PatchException("Could not find any class match")

    /**
     * Find a class with a predicate.
     *
     * @param classType The full classname.
     * @return An immutable instance of the class type.
     * @see mutableClassBy
     */
    fun classBy(classType: String) = classByOrNull(classType)
        ?: throw PatchException("Could not find class: $classType")

    /**
     * Mutable class from a full class name.
     * Returns `null` if class is not available, such as a built in Android or Java library.
     *
     * @param classDefType The full classname.
     * @return A mutable version of the class type.
     */
    fun mutableClassByOrNull(classDefType: String): MutableClass? {
        val poolValue = classMap[classDefType] ?: return null
        return poolValue.getMutableClass()
    }

    /**
     * Find a class with a predicate.
     *
     * @param classDefType The full classname.
     * @return A mutable version of the class type.
     */
    fun mutableClassBy(classDefType: String) = mutableClassByOrNull(classDefType)
        ?: throw PatchException("Could not find class: $classDefType")

    /**
     * Find a mutable class with a predicate.
     *
     * @param predicate A predicate to match the class.
     * @return A mutable class that matches the predicate.
     */
    fun mutableClassByOrNull(predicate: (ClassDef) -> Boolean) =
        poolValueByOrNull(predicate)?.getMutableClass()

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
    fun mutableClassBy(predicate: (ClassDef) -> Boolean) = mutableClassByOrNull(predicate)
        ?: throw PatchException("Could not find any class match")

    private fun getMethodsByStrings(): Map<String, List<PatchesClassMapValue>> {
        if (_stringMap != null) {
            return _stringMap!!
        }

        val map = HashMap<String, LinkedList<PatchesClassMapValue>>()

        classMap.values.forEach { poolEntry ->
            poolEntry.strings?.forEach { stringLiteral ->
                map.getOrPut(stringLiteral) {
                    LinkedList()
                }.add(poolEntry)
            }

            // String literals are no longer needed and can clear them.
            poolEntry.strings?.clear()
        }

        _stringMap = map
        return map
    }

    internal fun getMethodClassPairsForString(stringLiteral: String): List<PatchesClassMapValue>? {
        return getMethodsByStrings()[stringLiteral]
    }
}
