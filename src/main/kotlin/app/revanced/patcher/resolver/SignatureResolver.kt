package app.revanced.patcher.resolver

import app.revanced.patcher.cache.MethodMap
import app.revanced.patcher.proxy.ClassProxy
import app.revanced.patcher.signature.MethodSignature
import app.revanced.patcher.signature.PatternScanResult
import app.revanced.patcher.signature.SignatureResolverResult
import org.jf.dexlib2.Opcode
import org.jf.dexlib2.iface.ClassDef
import org.jf.dexlib2.iface.Method
import org.jf.dexlib2.iface.instruction.Instruction

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
                    val patternScanData = compareSignatureToMethod(signature, method) ?: continue

                    // create class proxy, in case a patch needs mutability
                    val classProxy = ClassProxy(classDef, index)
                    methodMap[signature.name] = SignatureResolverResult(
                        classProxy,
                        method.name,
                        patternScanData
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
                val result = compareSignatureToMethod(signature, method) ?: continue
                return SignatureResolverResult(
                    classProxy,
                    method.name,
                    result
                )
            }
            return null
        }

        private fun compareSignatureToMethod(
            signature: MethodSignature,
            method: Method
        ): PatternScanResult? {
            signature.returnType?.let { _ ->
                if (!method.returnType.startsWith(signature.returnType)) return@compareSignatureToMethod null
            }

            signature.accessFlags?.let { _ ->
                if (signature.accessFlags != method.accessFlags) {
                    return@compareSignatureToMethod null
                }
            }

            signature.methodParameters?.let { _ ->
                if (signature.methodParameters.all { signatureMethodParameter ->
                        method.parameterTypes.any { methodParameter ->
                            methodParameter.startsWith(signatureMethodParameter)
                        }
                    }) {
                    return@compareSignatureToMethod null
                }
            }

            return if (signature.opcodes == null) null
            else method.implementation?.instructions?.scanFor(signature.opcodes)!!
        }
    }
}

private operator fun ClassDef.component1() = this
private operator fun ClassDef.component2() = this.methods

private fun MutableIterable<Instruction>.scanFor(pattern: Array<Opcode>): PatternScanResult? {
    // TODO: create var for count?
    for (instructionIndex in 0 until this.count()) {
        var patternIndex = 0
        while (instructionIndex + patternIndex < this.count()) {
            if (this.elementAt(instructionIndex + patternIndex).opcode != pattern[patternIndex]) break
            if (++patternIndex < pattern.size) continue

            return PatternScanResult(instructionIndex, instructionIndex + patternIndex)
        }
    }

    return null
}