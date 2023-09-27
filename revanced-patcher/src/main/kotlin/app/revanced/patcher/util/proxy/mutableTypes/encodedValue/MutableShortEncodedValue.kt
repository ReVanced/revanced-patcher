package app.revanced.patcher.util.proxy.mutableTypes.encodedValue

import com.android.tools.smali.dexlib2.base.value.BaseShortEncodedValue
import com.android.tools.smali.dexlib2.iface.value.ShortEncodedValue

class MutableShortEncodedValue(shortEncodedValue: ShortEncodedValue) : BaseShortEncodedValue(), MutableEncodedValue {
    private var value = shortEncodedValue.value

    override fun getValue(): Short {
        return this.value
    }

    fun setValue(value: Short) {
        this.value = value
    }

    companion object {
        fun ShortEncodedValue.toMutable(): MutableShortEncodedValue {
            return MutableShortEncodedValue(this)
        }
    }
}
