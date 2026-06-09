package app.revanced.com.android.tools.smali.dexlib2.mutable

import app.revanced.com.android.tools.smali.dexlib2.mutable.MutableAnnotation
import com.android.tools.smali.dexlib2.base.BaseMethodParameter
import com.android.tools.smali.dexlib2.iface.MethodParameter

class MutableMethodParameter(
    parameter: MethodParameter,
) : BaseMethodParameter(),
    MethodParameter {
    private var type = parameter.type
    private var name = parameter.name
    private var signature = parameter.signature
    private val _annotations by lazy {
        parameter.annotations.map { annotation -> MutableAnnotation(annotation) }.toMutableSet()
    }

    override fun getType() = type

    override fun getName() = name

    override fun getSignature() = signature

    override fun getAnnotations() = _annotations

    companion object {
        @Deprecated(
            "Use MutableMethodParameter constructor instead.",
            ReplaceWith("MutableMethodParameter(this)")
        )
        fun MethodParameter.toMutable() = MutableMethodParameter(this)
    }
}
