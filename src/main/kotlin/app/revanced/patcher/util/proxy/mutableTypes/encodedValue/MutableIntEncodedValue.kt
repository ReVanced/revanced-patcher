package app.revanced.patcher.util.proxy.mutableTypes.encodedValue

import org.jf.dexlib2.base.value.BaseIntEncodedValue
import org.jf.dexlib2.iface.value.IntEncodedValue

class MutableIntEncodedValue(intEncodedValue: IntEncodedValue) : BaseIntEncodedValue(), MutableEncodedValue {
    private var value = intEncodedValue.value

    override fun getValue(): Int {
        return this.value
    }

    fun setValue(value: Int) {
        this.value = value
    }

    companion object {
        fun IntEncodedValue.toMutable(): MutableIntEncodedValue {
            return MutableIntEncodedValue(this)
        }
    }
}