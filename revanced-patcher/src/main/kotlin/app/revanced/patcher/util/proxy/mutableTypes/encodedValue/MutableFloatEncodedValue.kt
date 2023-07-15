package app.revanced.patcher.util.proxy.mutableTypes.encodedValue

import org.jf.dexlib2.base.value.BaseFloatEncodedValue
import org.jf.dexlib2.iface.value.FloatEncodedValue

class MutableFloatEncodedValue(floatEncodedValue: FloatEncodedValue) : BaseFloatEncodedValue(), MutableEncodedValue {
    private var value = floatEncodedValue.value

    override fun getValue(): Float {
        return this.value
    }

    fun setValue(value: Float) {
        this.value = value
    }

    companion object {
        fun FloatEncodedValue.toMutable(): MutableFloatEncodedValue {
            return MutableFloatEncodedValue(this)
        }
    }
}