package com.android.tools.smali.dexlib2.iface.value

import com.android.tools.smali.dexlib2.base.value.BaseMethodEncodedValue
import com.android.tools.smali.dexlib2.iface.reference.MethodReference

class MutableMethodEncodedValue(methodEncodedValue: MethodEncodedValue) :
    BaseMethodEncodedValue(),
    MutableEncodedValue {
    private var value = methodEncodedValue.value

    fun setValue(value: MethodReference) {
        this.value = value
    }

    override fun getValue(): MethodReference = this.value

    companion object {
        fun MethodEncodedValue.toMutable(): MutableMethodEncodedValue = MutableMethodEncodedValue(this)
    }
}
