package app.revanced.patcher.util.proxy.mutableTypes

import app.revanced.patcher.util.proxy.mutableTypes.MutableAnnotation.Companion.toMutable
import com.android.tools.smali.dexlib2.base.BaseMethodParameter
import com.android.tools.smali.dexlib2.iface.MethodParameter

// TODO: finish overriding all members if necessary
class MutableMethodParameter(parameter: MethodParameter) :
    BaseMethodParameter(),
    MethodParameter {
    private var type = parameter.type
    private var name = parameter.name
    private var signature = parameter.signature
    private val _annotations by lazy {
        parameter.annotations.map { annotation -> annotation.toMutable() }.toMutableSet()
    }

    override fun getType(): String = type

    override fun getName(): String? = name

    override fun getSignature(): String? = signature

    override fun getAnnotations(): MutableSet<MutableAnnotation> = _annotations

    companion object {
        fun MethodParameter.toMutable(): MutableMethodParameter = MutableMethodParameter(this)
    }
}
