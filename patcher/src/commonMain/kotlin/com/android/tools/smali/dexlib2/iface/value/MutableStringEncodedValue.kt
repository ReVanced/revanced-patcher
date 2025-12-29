package com.android.tools.smali.dexlib2.iface.value

import com.android.tools.smali.dexlib2.base.value.BaseStringEncodedValue

class MutableStringEncodedValue(stringEncodedValue: StringEncodedValue) :
    BaseStringEncodedValue(),
    MutableEncodedValue {
    private var value = stringEncodedValue.value

    fun setValue(value: String) {
        this.value = value
    }

    override fun getValue(): String = this.value

    companion object {
        fun ByteEncodedValue.toMutable(): MutableByteEncodedValue = MutableByteEncodedValue(this)
    }
}
