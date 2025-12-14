package com.android.tools.smali.dexlib2.iface.value

import com.android.tools.smali.dexlib2.ValueType
import com.android.tools.smali.dexlib2.base.value.BaseFieldEncodedValue
import com.android.tools.smali.dexlib2.iface.reference.FieldReference

class MutableFieldEncodedValue(fieldEncodedValue: FieldEncodedValue) : BaseFieldEncodedValue(), MutableEncodedValue {
    private var value = fieldEncodedValue.value

    fun setValue(value: FieldReference) {
        this.value = value
    }

    override fun getValueType(): Int = ValueType.FIELD

    override fun getValue(): FieldReference = this.value

    companion object {
        fun FieldEncodedValue.toMutable(): MutableFieldEncodedValue = MutableFieldEncodedValue(this)
    }
}
