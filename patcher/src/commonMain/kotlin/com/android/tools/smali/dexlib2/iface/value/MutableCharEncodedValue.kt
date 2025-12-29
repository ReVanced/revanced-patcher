package com.android.tools.smali.dexlib2.iface.value

import com.android.tools.smali.dexlib2.base.value.BaseCharEncodedValue

class MutableCharEncodedValue(charEncodedValue: CharEncodedValue) : BaseCharEncodedValue(), MutableEncodedValue {
    private var value = charEncodedValue.value

    fun setValue(value: Char) {
        this.value = value
    }

    override fun getValue(): Char = this.value

    companion object {
        fun CharEncodedValue.toMutable(): MutableCharEncodedValue = MutableCharEncodedValue(this)
    }
}
