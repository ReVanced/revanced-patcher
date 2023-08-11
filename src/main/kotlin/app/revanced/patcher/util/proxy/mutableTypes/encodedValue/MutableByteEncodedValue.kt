package app.revanced.patcher.util.proxy.mutableTypes.encodedValue

import com.android.tools.smali.dexlib2.base.value.BaseByteEncodedValue
import com.android.tools.smali.dexlib2.iface.value.ByteEncodedValue

class MutableByteEncodedValue(byteEncodedValue: ByteEncodedValue) : BaseByteEncodedValue(), MutableEncodedValue {
    private var value = byteEncodedValue.value

    override fun getValue(): Byte {
        return this.value
    }

    fun setValue(value: Byte) {
        this.value = value
    }

    companion object {
        fun ByteEncodedValue.toMutable(): MutableByteEncodedValue {
            return MutableByteEncodedValue(this)
        }
    }
}