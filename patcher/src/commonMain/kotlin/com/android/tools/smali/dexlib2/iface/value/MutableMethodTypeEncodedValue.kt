package com.android.tools.smali.dexlib2.iface.value

import com.android.tools.smali.dexlib2.base.value.BaseMethodTypeEncodedValue
import com.android.tools.smali.dexlib2.iface.reference.MethodProtoReference

class MutableMethodTypeEncodedValue(methodTypeEncodedValue: MethodTypeEncodedValue) : BaseMethodTypeEncodedValue(), MutableEncodedValue {
    private var value = methodTypeEncodedValue.value

    fun setValue(value: MethodProtoReference) {
        this.value = value
    }

    override fun getValue(): MethodProtoReference = this.value

    companion object {
        fun MethodTypeEncodedValue.toMutable(): MutableMethodTypeEncodedValue = MutableMethodTypeEncodedValue(this)
    }
}
