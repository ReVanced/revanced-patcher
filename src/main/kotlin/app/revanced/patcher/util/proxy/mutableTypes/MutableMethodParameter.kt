package app.revanced.patcher.util.proxy.mutableTypes

import app.revanced.patcher.util.proxy.mutableTypes.MutableAnnotation.Companion.toMutable
import org.jf.dexlib2.base.BaseMethodParameter
import org.jf.dexlib2.iface.MethodParameter

// TODO: Finish overriding all members if necessary.
class MutableMethodParameter(parameter: MethodParameter) : MethodParameter, BaseMethodParameter() {
    private var type = parameter.type
    private var name = parameter.name
    private var signature = parameter.signature
    private val _annotations by lazy {
        parameter.annotations.map { annotation -> annotation.toMutable() }.toMutableSet()
    }

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
        return _annotations
    }

    companion object {
        fun MethodParameter.toMutable(): MutableMethodParameter {
            return MutableMethodParameter(this)
        }
    }
}
