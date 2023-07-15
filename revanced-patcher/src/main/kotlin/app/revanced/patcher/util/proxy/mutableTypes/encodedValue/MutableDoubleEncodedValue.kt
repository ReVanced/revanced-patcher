package app.revanced.patcher.util.proxy.mutableTypes.encodedValue

import org.jf.dexlib2.base.value.BaseDoubleEncodedValue
import org.jf.dexlib2.iface.value.DoubleEncodedValue

class MutableDoubleEncodedValue(doubleEncodedValue: DoubleEncodedValue) : BaseDoubleEncodedValue(),
    MutableEncodedValue {
    private var value = doubleEncodedValue.value

    override fun getValue(): Double {
        return this.value
    }

    fun setValue(value: Double) {
        this.value = value
    }

    companion object {
        fun DoubleEncodedValue.toMutable(): MutableDoubleEncodedValue {
            return MutableDoubleEncodedValue(this)
        }
    }
}