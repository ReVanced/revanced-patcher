package com.android.tools.smali.dexlib2.iface.value

import com.android.tools.smali.dexlib2.base.value.BaseDoubleEncodedValue

class MutableDoubleEncodedValue(doubleEncodedValue: DoubleEncodedValue) :
    BaseDoubleEncodedValue(),
    MutableEncodedValue {
    private var value = doubleEncodedValue.value

    fun setValue(value: Double) {
        this.value = value
    }

    override fun getValue(): Double = this.value

    companion object {
        fun DoubleEncodedValue.toMutable(): MutableDoubleEncodedValue = MutableDoubleEncodedValue(this)
    }
}
