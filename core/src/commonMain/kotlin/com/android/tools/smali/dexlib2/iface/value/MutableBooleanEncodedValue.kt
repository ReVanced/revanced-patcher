package com.android.tools.smali.dexlib2.iface.value

import com.android.tools.smali.dexlib2.base.value.BaseBooleanEncodedValue

class MutableBooleanEncodedValue(booleanEncodedValue: BooleanEncodedValue) :
    BaseBooleanEncodedValue(),
    MutableEncodedValue {
    private var value = booleanEncodedValue.value

    fun setValue(value: Boolean) {
        this.value = value
    }

    override fun getValue(): Boolean = this.value

    companion object {
        fun BooleanEncodedValue.toMutable(): MutableBooleanEncodedValue = MutableBooleanEncodedValue(this)
    }
}
