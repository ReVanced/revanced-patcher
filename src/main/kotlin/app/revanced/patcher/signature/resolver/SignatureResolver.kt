package app.revanced.patcher.signature.resolver

import app.revanced.patcher.proxy.ClassProxy
import app.revanced.patcher.signature.MethodSignature
import app.revanced.patcher.signature.PatternScanMethod
import app.revanced.patcher.signature.PatternScanResult
import app.revanced.patcher.signature.SignatureResolverResult
import org.jf.dexlib2.iface.ClassDef
import org.jf.dexlib2.iface.Method
import org.jf.dexlib2.iface.instruction.Instruction

internal class SignatureResolver(
    private val classes: List<ClassDef>,
    private val methodSignatures: Iterable<MethodSignature>
) {
    fun resolve() {
        for ((index, classDef) in classes.withIndex()) {
            for (signature in methodSignatures) {
                for (method in classDef.methods) {
                    val patternScanData = compareSignatureToMethod(signature, method) ?: continue

                    // create class proxy, in case a patch needs mutability
                    val classProxy = ClassProxy(classDef, index)
                    signature.result = SignatureResolverResult(
                        classProxy,
                        patternScanData,
                        method.name,
                    )
                }
            }
        }
    }

    // These functions do not require the constructor values, so they can be static.
    companion object {
        fun resolveFromProxy(classProxy: ClassProxy, signature: MethodSignature): SignatureResolverResult? {
            for (method in classProxy.immutableClass.methods) {
                val result = compareSignatureToMethod(signature, method) ?: continue
                return SignatureResolverResult(
                    classProxy,
                    result,
                    method.name,
                )
            }
            return null
        }

        private fun compareSignatureToMethod(
            signature: MethodSignature,
            method: Method
        ): PatternScanResult? {
            signature.returnType?.let {
                if (!method.returnType.startsWith(signature.returnType)) {
                    return null
                }
            }

            signature.accessFlags?.let {
                if (signature.accessFlags != method.accessFlags) {
                    return null
                }
            }

            signature.methodParameters?.let {
                if (compareParameterTypes(signature.methodParameters, method.parameterTypes)) {
                    return null
                }
            }

            return if (signature.opcodes == null) {
                PatternScanResult(0, 0)
            } else {
                method.implementation?.instructions?.let {
                    compareOpcodes(signature, it)
                }
            }
        }

        private fun compareOpcodes(
            signature: MethodSignature,
            instructions: Iterable<Instruction>
        ): PatternScanResult? {
            val count = instructions.count()
            val pattern = signature.opcodes!!
            val size = pattern.count()
            val method = signature.metadata.patternScanMethod
            val threshold = if (method is PatternScanMethod.Fuzzy)
                method.threshold else 0

            for (instructionIndex in 0 until count) {
                var patternIndex = 0
                var currentThreshold = threshold
                while (instructionIndex + patternIndex < count) {
                    val originalOpcode = instructions.elementAt(instructionIndex + patternIndex).opcode
                    val patternOpcode = pattern.elementAt(patternIndex)
                    if (
                        patternOpcode != null && // unknown opcode
                        originalOpcode != patternOpcode &&
                        currentThreshold-- == 0
                    ) break
                    if (++patternIndex < size) continue

                    val result = PatternScanResult(instructionIndex, instructionIndex + patternIndex)
                    if (method is PatternScanMethod.Fuzzy) {
                        method.warnings = generateWarnings(
                            signature, instructions, result
                        )
                    }
                    return result
                }
            }

            return null
        }

        private fun compareParameterTypes(
            signature: Iterable<String>,
            original: MutableList<out CharSequence>
        ): Boolean {
            return signature.count() != original.size || !(signature.all { a -> original.any { it.startsWith(a) } })
        }

        private fun generateWarnings(
            signature: MethodSignature,
            instructions: Iterable<Instruction>,
            scanResult: PatternScanResult,
        ) = buildList {
            val pattern = signature.opcodes!!
            for ((patternIndex, originalIndex) in (scanResult.startIndex until scanResult.endIndex).withIndex()) {
                val originalOpcode = instructions.elementAt(originalIndex).opcode
                val patternOpcode = pattern.elementAt(patternIndex)
                if (
                    patternOpcode != null && // unknown opcode
                    originalOpcode != patternOpcode
                ) {
                    this.add(
                        PatternScanMethod.Fuzzy.Warning(
                            patternOpcode, originalOpcode,
                            patternIndex, originalIndex,
                        )
                    )
                }
            }
        }
    }
}

private operator fun ClassDef.component1() = this
private operator fun ClassDef.component2() = this.methods