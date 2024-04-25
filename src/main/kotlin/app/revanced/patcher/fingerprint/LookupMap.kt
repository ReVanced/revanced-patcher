package app.revanced.patcher.fingerprint

import app.revanced.patcher.patch.BytecodePatchContext
import com.android.tools.smali.dexlib2.Opcode
import com.android.tools.smali.dexlib2.iface.ClassDef
import com.android.tools.smali.dexlib2.iface.Method
import com.android.tools.smali.dexlib2.iface.instruction.ReferenceInstruction
import com.android.tools.smali.dexlib2.iface.reference.StringReference
import java.util.*

internal typealias MethodClassPair = Pair<Method, ClassDef>

/**
 * Lookup map for methods.
 */
internal class LookupMap : MutableMap<String, LookupMap.MethodClassList> by mutableMapOf() {
    /**
     * Adds a [MethodClassPair] to the list associated with the given key.
     * If the key does not exist, a new list is created and the [MethodClassPair] is added to it.
     */
    fun add(
        key: String,
        methodClassPair: MethodClassPair,
    ) {
        getOrPut(key) { MethodClassList() }.add(methodClassPair)
    }

    /**
     * List of methods and the class they are a member of.
     */
    internal class MethodClassList : LinkedList<MethodClassPair>()

    companion object Maps {
        /**
         * A list of methods and the class they are a member of.
         */
        internal val methods = MethodClassList()

        /**
         * Lookup map for methods keyed to the methods access flags, return type and parameter.
         */
        internal val methodSignatureLookupMap = LookupMap()

        /**
         * Lookup map for methods associated by strings referenced in the method.
         */
        internal val methodStringsLookupMap = LookupMap()

        /**
         * Initializes lookup maps for [MethodFingerprint] resolution
         * using attributes of methods such as the method signature or strings.
         *
         * @param context The [BytecodePatchContext] containing the classes to initialize the lookup maps with.
         */
        internal fun initializeLookupMaps(context: BytecodePatchContext) {
            if (methods.isNotEmpty()) clearLookupMaps()

            context.classes.forEach { classDef ->
                classDef.methods.forEach { method ->
                    val methodClassPair = method to classDef

                    // For fingerprints with no access or return type specified.
                    methods += methodClassPair

                    val accessFlagsReturnKey = method.accessFlags.toString() + method.returnType.first()

                    // Add <access><returnType> as the key.
                    methodSignatureLookupMap.add(accessFlagsReturnKey, methodClassPair)

                    // Add <access><returnType>[parameters] as the key.
                    methodSignatureLookupMap.add(
                        buildString {
                            append(accessFlagsReturnKey)
                            appendParameters(method.parameterTypes)
                        },
                        methodClassPair,
                    )

                    // Add strings contained in the method as the key.
                    method.implementation?.instructions?.forEach instructions@{ instruction ->
                        if (instruction.opcode != Opcode.CONST_STRING && instruction.opcode != Opcode.CONST_STRING_JUMBO) {
                            return@instructions
                        }

                        val string = ((instruction as ReferenceInstruction).reference as StringReference).string

                        methodStringsLookupMap.add(string, methodClassPair)
                    }

                    // In the future, the class type could be added to the lookup map.
                    // This would require MethodFingerprint to be changed to include the class type.
                }
            }
        }

        /**
         * Clears the internal lookup maps created in [initializeLookupMaps].
         */
        internal fun clearLookupMaps() {
            methods.clear()
            methodSignatureLookupMap.clear()
            methodStringsLookupMap.clear()
        }

        /**
         * Appends a string based on the parameter reference types of this method.
         */
        internal fun StringBuilder.appendParameters(parameters: Iterable<CharSequence>) {
            // Maximum parameters to use in the signature key.
            // Some apps have methods with an incredible number of parameters (over 100 parameters have been seen).
            // To keep the signature map from becoming needlessly bloated,
            // group together in the same map entry all methods with the same access/return and 5 or more parameters.
            // The value of 5 was chosen based on local performance testing and is not set in stone.
            val maxSignatureParameters = 5
            // Must append a unique value before the parameters to distinguish this key includes the parameters.
            // If this is not appended, then methods with no parameters
            // will collide with different keys that specify access/return but omit the parameters.
            append("p:")
            parameters.forEachIndexed { index, parameter ->
                if (index >= maxSignatureParameters) return
                append(parameter.first())
            }
        }
    }
}
