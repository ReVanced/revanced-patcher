package app.revanced.patcher.util.proxy.mutableTypes.encodedValue

import com.android.tools.smali.dexlib2.base.value.BaseCharEncodedValue
import com.android.tools.smali.dexlib2.iface.value.CharEncodedValue

class MutableCharEncodedValue(charEncodedValue: CharEncodedValue) : BaseCharEncodedValue(), MutableEncodedValue {
    private var value = charEncodedValue.value

    override fun getValue(): Char {
        return this.value
    }

    fun setValue(value: Char) {
        this.value = value
    }

    companion object {
        fun CharEncodedValue.toMutable(): MutableCharEncodedValue {
            return MutableCharEncodedValue(this)
        }
    }
}
