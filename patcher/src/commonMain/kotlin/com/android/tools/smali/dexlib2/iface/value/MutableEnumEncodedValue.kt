package com.android.tools.smali.dexlib2.iface.value

import com.android.tools.smali.dexlib2.base.value.BaseEnumEncodedValue
import com.android.tools.smali.dexlib2.iface.reference.FieldReference

class MutableEnumEncodedValue(enumEncodedValue: EnumEncodedValue) : BaseEnumEncodedValue(), MutableEncodedValue {
    private var value = enumEncodedValue.value

    fun setValue(value: FieldReference) {
        this.value = value
    }

    override fun getValue(): FieldReference = this.value

    companion object {
        fun EnumEncodedValue.toMutable(): MutableEnumEncodedValue = MutableEnumEncodedValue(this)
    }
}
