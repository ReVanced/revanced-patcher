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
     * Class type -> ClassDef.
     */
    internal val classMap: MutableMap<String, ClassDefWrapper>
) {

    /**
     * Container to hold the class definition that is either mutable or immutable.
     *
     * This intermediate container is needed to easily update the class in both
     * the class map and in the string map with a single constant time operation.
     */
    internal class ClassDefWrapper(
        /**
         * Can be immutable or mutable.
         */
        var classDef: ClassDef,
    ) {
        fun getMutableClass(): MutableClass {
            if (classDef !is MutableClass) {
                classDef = MutableClass(classDef)
            }
            return classDef as MutableClass
        }
    }

    /**
     * @return All strings found anywhere in all class methods.
     */
    private fun ClassDef.findMethodStrings(): List<String>? {
        var list : MutableList<String>? = null

        methods.forEach { method ->
            // Add strings contained in the method as the key.
            method.instructionsOrNull?.forEach { instruction ->
                val opcode = instruction.opcode
                if (opcode != Opcode.CONST_STRING && opcode != Opcode.CONST_STRING_JUMBO) {
                    return@forEach
                }

                val string = ((instruction as ReferenceInstruction).reference as StringReference).string

                if (list == null) {
                    list = mutableListOf()
                }
                list.add(string)
            }
        }

        return list
    }

    /**
     * Opcode string constant -> ClassDef that contains the method.
     */
    private var stringMap: Map<String, List<ClassDefWrapper>>? = null

    /**
     * All classes that contain at least 1 string.
     * Same contents as [stringMap] except contains no duplicates.
     */
    private var allClassesWithStrings: List<ClassDefWrapper>? = null

    internal constructor(set: Set<ClassDef>) : this(set.map {
        ClassDefWrapper(it)
    }.associateByTo(
        // Must use linked hash map, otherwise with a regular map the ordering of classes found
        // in the apk is not preserved, and old fingerprints that have multiple matches can match
        // the wrong class due to hashmap random class iteration during matching. The issue is with
        // some fingerprint declarations not being unique enough and currently there is no way to
        // check for duplicate matches.
        // See https://github.com/ReVanced/revanced-patcher/issues/74
        //
        // Pre-size so rehashing doesn't occur and use a more performant load factor.
        LinkedHashMap(2 * set.size, 0.5f)
    ) { classDefStrings ->
        classDefStrings.classDef.type
    })

    internal fun close() {
        classMap.clear()
        closeStringMap()
    }

    internal fun closeStringMap() {
        stringMap = null
        allClassesWithStrings = null
    }

    internal fun addClass(classDef: ClassDef) {
        classMap[classDef.type] = ClassDefWrapper(classDef)
    }

    private fun getClassesByStrings(): Map<String, List<ClassDefWrapper>> {
        if (stringMap != null) {
            return stringMap!!
        }

        // Default 0.75f load factor works well and a lower value does not improve patching time.
        val map = HashMap<String, LinkedList<ClassDefWrapper>>()
        val allClasses = mutableListOf<ClassDefWrapper>()

        classMap.values.forEach { wrapper ->
            wrapper.classDef.findMethodStrings()?.forEach { stringLiteral ->
                map.getOrPut(stringLiteral) {
                    LinkedList()
                }.add(wrapper)
            }

            allClasses += wrapper
        }

        stringMap = map
        allClassesWithStrings = allClasses
        return map
    }

    internal fun getClassFromOpcodeStringLiteral(stringLiteral: String): List<ClassDefWrapper>? {
        return getClassesByStrings()[stringLiteral]
    }

    internal fun getAllClassesWithStrings(): List<ClassDefWrapper> {
        getClassesByStrings() // Load string map if needed.
        return allClassesWithStrings!!
    }

    /**
     * Iterate over all classes.
     */
    fun forEach(action: (ClassDef) -> Unit) {
        classMap.values.forEach { wrapper ->
            action(wrapper.classDef)
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

    private fun mapWrapperByOrNull(predicate: (ClassDef) -> Boolean) =
        classMap.values.find { wrapper ->
            predicate(wrapper.classDef)
        }

    /**
     * Find a class with a predicate. If you know the class type name,
     * it is highly preferred to instead use [classByOrNull(String)].
     *
     * @param predicate A predicate to match the class.
     * @return An immutable instance of the class type, or null if not found.
     */
    fun classByOrNull(predicate: (ClassDef) -> Boolean) = mapWrapperByOrNull(predicate)?.classDef

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
        val wrapper = classMap[classDefType] ?: return null
        return wrapper.getMutableClass()
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
        mapWrapperByOrNull(predicate)?.getMutableClass()

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
}
