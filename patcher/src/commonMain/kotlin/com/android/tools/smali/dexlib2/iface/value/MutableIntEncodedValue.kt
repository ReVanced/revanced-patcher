package com.android.tools.smali.dexlib2.iface.value

import com.android.tools.smali.dexlib2.base.value.BaseIntEncodedValue

class MutableIntEncodedValue(intEncodedValue: IntEncodedValue) : BaseIntEncodedValue(), MutableEncodedValue {
    private var value = intEncodedValue.value

    fun setValue(value: Int) {
        this.value = value
    }

    override fun getValue(): Int = this.value

    companion object {
        fun IntEncodedValue.toMutable(): MutableIntEncodedValue = MutableIntEncodedValue(this)
    }
}
