package app.revanced.patcher.resolver

import app.revanced.patcher.cache.MethodMap
import app.revanced.patcher.signature.MethodSignatureScanResult
import app.revanced.patcher.signature.PatternScanData
import app.revanced.patcher.signature.MethodSignature
import org.jf.dexlib2.Opcode
import org.jf.dexlib2.iface.ClassDef
import org.jf.dexlib2.iface.Method

// TODO: add logger
internal class MethodResolver(private val classes: Set<ClassDef>, private val signatures: Array<MethodSignature>) {
    fun resolve(): MethodMap {
        val methodMap = MethodMap()

        for (classDef in classes) {
            for (method in classDef.methods) {
                for (methodSignature in signatures) {
                    if (methodMap.containsKey(methodSignature.name)) { // method already found for this sig
                        continue
                    }

                    val (r, sr) = cmp(method, methodSignature)
                    if (!r || sr == null) {
                        continue
                    }

                    methodMap[methodSignature.name] = MethodSignatureScanResult(
                        method,
                        PatternScanData(
                            // sadly we cannot create contracts for a data class, so we must assert
                            sr.startIndex!!,
                            sr.endIndex!!
                        )
                    )
                }
            }
        }

        for (signature in signatures) {
            if (methodMap.containsKey(signature.name)) continue
        }

        return methodMap
    }

    // These functions do not require the constructor values, so they can be static.
    companion object {
        fun resolveMethod(classNode: ClassDef, signature: MethodSignature): MethodSignatureScanResult? {
            for (method in classNode.methods) {
                val (r, sr) = cmp(method, signature)
                if (!r || sr == null) continue
                return MethodSignatureScanResult(
                    method,
                    PatternScanData(0, 0) // opcode list is always ignored.
                )
            }
            return null
        }

        private fun cmp(method: Method, signature: MethodSignature): Pair<Boolean, MethodResolverScanResult?> {
            // TODO: compare as generic object if not primitive
            signature.returnType?.let { _ ->
                if (signature.returnType != method.returnType) {
                    return@cmp false to null
                }
            }

            signature.accessFlags?.let { _ ->
                if (signature.accessFlags != method.accessFlags) {
                    return@cmp false to null
                }
            }

            // TODO: compare as generic object if the parameter is not primitive
            signature.methodParameters?.let { _ ->
                if (signature.methodParameters != method.parameters) {
                    return@cmp false to null
                }
            }

            signature.opcodes?.let { _ ->
                val result = method.implementation?.instructions?.scanFor(signature.opcodes)
                return@cmp if (result != null && result.found) true to result else false to null
            }

            return true to MethodResolverScanResult(true)
        }
    }
}

private operator fun ClassDef.component1() = this
private operator fun ClassDef.component2() = this.methods

private fun <T> MutableIterable<T>.scanFor(pattern: Array<Opcode>): MethodResolverScanResult {
    // TODO: create var for count?
    for (i in 0 until this.count()) {
        var occurrence = 0
        while (i + occurrence < this.count()) {
            val n = this.elementAt(i + occurrence)
            if (!n.shouldSkip() && n != pattern[occurrence]) break
            if (++occurrence >= pattern.size) {
                val current = i + occurrence
                return MethodResolverScanResult(true, current - pattern.size, current)
            }
        }
    }

    return MethodResolverScanResult(false)
}

// TODO: extend Opcode type, not T (requires a cast to Opcode)
private fun <T> T.shouldSkip(): Boolean {
    return this == Opcode.GOTO // TODO: and: this == AbstractInsnNode.LINE
}

// TODO: use this somehow to compare types as generic objects if not primitive
// private fun Type.convertObject(): Type {
//     return when (this.sort) {
//         Type.OBJECT -> ExtraTypes.Any
//         Type.ARRAY -> ExtraTypes.ArrayAny
//         else -> this
//     }
// }
//
// private fun Array<Type>.convertObjects(): Array<Type> {
//     return this.map { it.convertObject() }.toTypedArray()
// }

