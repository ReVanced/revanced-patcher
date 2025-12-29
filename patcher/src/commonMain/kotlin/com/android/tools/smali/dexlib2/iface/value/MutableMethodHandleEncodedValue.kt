package com.android.tools.smali.dexlib2.iface.value

import com.android.tools.smali.dexlib2.base.value.BaseMethodHandleEncodedValue
import com.android.tools.smali.dexlib2.iface.reference.MethodHandleReference

class MutableMethodHandleEncodedValue(methodHandleEncodedValue: MethodHandleEncodedValue) :
    BaseMethodHandleEncodedValue(),
    MutableEncodedValue {
    private var value = methodHandleEncodedValue.value

    fun setValue(value: MethodHandleReference) {
        this.value = value
    }

    override fun getValue(): MethodHandleReference = this.value

    companion object {
        fun MethodHandleEncodedValue.toMutable(): MutableMethodHandleEncodedValue = MutableMethodHandleEncodedValue(this)
    }
}
