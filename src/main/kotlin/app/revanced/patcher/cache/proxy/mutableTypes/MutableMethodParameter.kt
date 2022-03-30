package app.revanced.patcher.cache.proxy.mutableTypes

import app.revanced.patcher.cache.proxy.mutableTypes.MutableAnnotation.Companion.toMutable
import org.jf.dexlib2.base.BaseMethodParameter
import org.jf.dexlib2.iface.MethodParameter

// TODO: finish overriding all members if necessary
class MutableMethodParameter(parameter: MethodParameter) : MethodParameter, BaseMethodParameter() {
    private var type = parameter.type
    private var name = parameter.name
    private var signature = parameter.signature
    private val annotations = parameter.annotations.map { annotation -> annotation.toMutable() }.toMutableSet()

    override fun getType(): String {
        return type
    }

    override fun getName(): String? {
        return name
    }

    override fun getSignature(): String? {
        return signature
    }

    override fun getAnnotations(): MutableSet<MutableAnnotation> {
        return annotations
    }

    companion object {
        fun MethodParameter.toMutable(): MutableMethodParameter {
            return MutableMethodParameter(this)
        }
    }
}
