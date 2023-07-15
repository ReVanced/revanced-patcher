package app.revanced.patcher.util.proxy.mutableTypes.encodedValue

import org.jf.dexlib2.base.value.BaseByteEncodedValue
import org.jf.dexlib2.iface.value.ByteEncodedValue

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