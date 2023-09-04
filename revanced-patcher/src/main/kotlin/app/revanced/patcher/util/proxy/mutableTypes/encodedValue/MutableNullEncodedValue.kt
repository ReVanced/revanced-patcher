package app.revanced.patcher.util.proxy.mutableTypes.encodedValue

import com.android.tools.smali.dexlib2.base.value.BaseNullEncodedValue
import com.android.tools.smali.dexlib2.iface.value.ByteEncodedValue

class MutableNullEncodedValue : BaseNullEncodedValue(), MutableEncodedValue {
    companion object {
        fun ByteEncodedValue.toMutable(): MutableByteEncodedValue {
            return MutableByteEncodedValue(this)
        }
    }
}