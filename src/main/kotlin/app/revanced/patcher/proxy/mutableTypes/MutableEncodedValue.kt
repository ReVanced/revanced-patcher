package app.revanced.patcher.proxy.mutableTypes

import org.jf.dexlib2.iface.value.EncodedValue

class MutableEncodedValue(encodedValue: EncodedValue) : EncodedValue {
    private var valueType = encodedValue.valueType

    fun setValueType(valueType: Int) {
        this.valueType = valueType
    }

    override fun compareTo(other: EncodedValue): Int {
        return valueType - other.valueType
    }

    override fun getValueType(): Int {
        return valueType
    }

    companion object {
        fun EncodedValue.toMutable(): MutableEncodedValue {
            return MutableEncodedValue(this)
        }
    }
}