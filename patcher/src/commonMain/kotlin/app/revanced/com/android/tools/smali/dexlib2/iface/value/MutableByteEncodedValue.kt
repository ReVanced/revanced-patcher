package app.revanced.com.android.tools.smali.dexlib2.iface.value

import com.android.tools.smali.dexlib2.base.value.BaseByteEncodedValue
import com.android.tools.smali.dexlib2.iface.value.ByteEncodedValue

class MutableByteEncodedValue(
    byteEncodedValue: ByteEncodedValue,
) : BaseByteEncodedValue(),
    MutableEncodedValue {
    private var value = byteEncodedValue.value

    fun setValue(value: Byte) {
        this.value = value
    }

    override fun getValue(): Byte = this.value

    companion object {
        fun ByteEncodedValue.toMutable(): MutableByteEncodedValue = MutableByteEncodedValue(this)
    }
}
