package app.revanced.patcher.resolver

import app.revanced.patcher.cache.MethodMap
import app.revanced.patcher.proxy.ClassProxy
import app.revanced.patcher.signature.MethodSignature
import app.revanced.patcher.signature.PatternScanResult
import app.revanced.patcher.signature.SignatureResolverResult
import org.jf.dexlib2.Opcode
import org.jf.dexlib2.iface.ClassDef
import org.jf.dexlib2.iface.Method

// TODO: add logger back
internal class SignatureResolver(
    private val classes: Set<ClassDef>,
    private val methodSignatures: Array<MethodSignature>
) {
    fun resolve(): MethodMap {
        val methodMap = MethodMap()

        for ((index, classDef) in classes.withIndex()) {
            for (signature in methodSignatures) {
                if (methodMap.containsKey(signature.name)) {
                    continue
                }

                for (method in classDef.methods) {
                    val (isMatch, patternScanData) = compareSignatureToMethod(signature, method)

                    if (!isMatch || patternScanData == null) {
                        continue
                    }

                    // create class proxy, in case a patch needs mutability
                    val classProxy = ClassProxy(classDef, index)
                    methodMap[signature.name] = SignatureResolverResult(
                        classProxy,
                        method.name,
                        PatternScanResult(
                            patternScanData.startIndex!!,
                            patternScanData.endIndex!!
                        )
                    )
                }
            }
        }

        // TODO: remove?
        for (signature in methodSignatures) {
            if (methodMap.containsKey(signature.name)) continue
        }

        return methodMap
    }

    // These functions do not require the constructor values, so they can be static.
    companion object {
        fun resolveFromProxy(classProxy: ClassProxy, signature: MethodSignature): SignatureResolverResult? {
            for (method in classProxy.immutableClass.methods) {
                val (r, sr) = compareSignatureToMethod(signature, method)
                if (!r || sr == null) continue
                return SignatureResolverResult(
                    classProxy,
                    method.name,
                    null
                )
            }
            return null
        }

        private fun compareSignatureToMethod(
            signature: MethodSignature,
            method: Method
        ): Pair<Boolean, PatternScanData?> {
            // TODO: compare as generic object if not primitive
            signature.returnType?.let { _ ->
                if (signature.returnType != method.returnType) {
                    return@compareSignatureToMethod false to null
                }
            }

            signature.accessFlags?.let { _ ->
                if (signature.accessFlags != method.accessFlags) {
                    return@compareSignatureToMethod false to null
                }
            }

            // TODO: compare as generic object if the parameter is not primitive
            signature.methodParameters?.let { _ ->
                if (signature.methodParameters != method.parameters) {
                    return@compareSignatureToMethod false to null
                }
            }

            signature.opcodes?.let { _ ->
                val result = method.implementation?.instructions?.scanFor(signature.opcodes)
                return@compareSignatureToMethod if (result != null && result.found) true to result else false to null
            }

            return true to PatternScanData(true)
        }
    }
}

private operator fun ClassDef.component1() = this
private operator fun ClassDef.component2() = this.methods

private fun <T> MutableIterable<T>.scanFor(pattern: Array<Opcode>): PatternScanData {
    // TODO: create var for count?
    for (i in 0 until this.count()) {
        var occurrence = 0
        while (i + occurrence < this.count()) {
            val n = this.elementAt(i + occurrence)
            if (!n.shouldSkip() && n != pattern[occurrence]) break
            if (++occurrence >= pattern.size) {
                val current = i + occurrence
                return PatternScanData(true, current - pattern.size, current)
            }
        }
    }

    return PatternScanData(false)
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

