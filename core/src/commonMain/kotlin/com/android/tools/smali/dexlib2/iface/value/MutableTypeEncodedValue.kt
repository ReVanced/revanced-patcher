package com.android.tools.smali.dexlib2.iface.value

import com.android.tools.smali.dexlib2.base.value.BaseTypeEncodedValue

class MutableTypeEncodedValue(typeEncodedValue: TypeEncodedValue) : BaseTypeEncodedValue(), MutableEncodedValue {
    private var value = typeEncodedValue.value

    fun setValue(value: String) {
        this.value = value
    }

    override fun getValue() = this.value

    companion object {
        fun TypeEncodedValue.toMutable(): MutableTypeEncodedValue = MutableTypeEncodedValue(this)
    }
}
