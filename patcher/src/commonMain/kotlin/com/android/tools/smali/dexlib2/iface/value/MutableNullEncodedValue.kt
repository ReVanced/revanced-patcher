package com.android.tools.smali.dexlib2.iface.value

import com.android.tools.smali.dexlib2.base.value.BaseNullEncodedValue

class MutableNullEncodedValue : BaseNullEncodedValue(), MutableEncodedValue {
    companion object {
        fun ByteEncodedValue.toMutable(): MutableByteEncodedValue = MutableByteEncodedValue(this)
    }
}
