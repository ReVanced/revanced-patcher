package app.revanced.patcher.util.proxy.mutableTypes.encodedValue

import org.jf.dexlib2.base.value.BaseStringEncodedValue
import org.jf.dexlib2.iface.value.ByteEncodedValue
import org.jf.dexlib2.iface.value.StringEncodedValue

class MutableStringEncodedValue(stringEncodedValue: StringEncodedValue) : BaseStringEncodedValue(),
    MutableEncodedValue {
    private var value = stringEncodedValue.value

    override fun getValue(): String {
        return this.value
    }

    fun setValue(value: String) {
        this.value = value
    }

    companion object {
        fun ByteEncodedValue.toMutable(): MutableByteEncodedValue {
            return MutableByteEncodedValue(this)
        }
    }
}