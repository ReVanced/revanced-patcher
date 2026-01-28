package app.revanced.com.android.tools.smali.dexlib2.iface.value

import com.android.tools.smali.dexlib2.base.value.BaseFloatEncodedValue
import com.android.tools.smali.dexlib2.iface.value.FloatEncodedValue

class MutableFloatEncodedValue(
    floatEncodedValue: FloatEncodedValue,
) : BaseFloatEncodedValue(),
    MutableEncodedValue {
    private var value = floatEncodedValue.value

    fun setValue(value: Float) {
        this.value = value
    }

    override fun getValue(): Float = this.value

    companion object {
        fun FloatEncodedValue.toMutable(): MutableFloatEncodedValue = MutableFloatEncodedValue(this)
    }
}
