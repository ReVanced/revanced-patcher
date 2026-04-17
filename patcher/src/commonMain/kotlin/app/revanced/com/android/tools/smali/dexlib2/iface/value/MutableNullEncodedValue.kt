package app.revanced.com.android.tools.smali.dexlib2.iface.value

import com.android.tools.smali.dexlib2.base.value.BaseNullEncodedValue
import com.android.tools.smali.dexlib2.iface.value.ByteEncodedValue

class MutableNullEncodedValue :
    BaseNullEncodedValue(),
    MutableEncodedValue {
    companion object {
        @Deprecated(
            "Use MutableByteEncodedValue constructor instead.",
            ReplaceWith("MutableByteEncodedValue(this)")
        )
        fun ByteEncodedValue.toMutable(): MutableByteEncodedValue = MutableByteEncodedValue(this)
    }
}
