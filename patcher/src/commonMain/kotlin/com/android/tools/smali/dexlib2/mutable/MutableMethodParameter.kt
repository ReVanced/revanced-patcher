package com.android.tools.smali.dexlib2.mutable

import com.android.tools.smali.dexlib2.base.BaseMethodParameter
import com.android.tools.smali.dexlib2.iface.MethodParameter
import com.android.tools.smali.dexlib2.mutable.MutableAnnotation.Companion.toMutable

class MutableMethodParameter(parameter: MethodParameter) : BaseMethodParameter(), MethodParameter {
    private var type = parameter.type
    private var name = parameter.name
    private var signature = parameter.signature
    private val _annotations by lazy {
        parameter.annotations.map { annotation -> annotation.toMutable() }.toMutableSet()
    }

    override fun getType() = type

    override fun getName() = name

    override fun getSignature() = signature

    override fun getAnnotations() = _annotations

    companion object {
        fun MethodParameter.toMutable() = MutableMethodParameter(this)
    }
}
