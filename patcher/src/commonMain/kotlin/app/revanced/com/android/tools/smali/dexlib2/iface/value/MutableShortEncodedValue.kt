package app.revanced.com.android.tools.smali.dexlib2.iface.value

import com.android.tools.smali.dexlib2.base.value.BaseShortEncodedValue
import com.android.tools.smali.dexlib2.iface.value.ShortEncodedValue

class MutableShortEncodedValue(
    shortEncodedValue: ShortEncodedValue,
) : BaseShortEncodedValue(),
    MutableEncodedValue {
    private var value = shortEncodedValue.value

    fun setValue(value: Short) {
        this.value = value
    }

    override fun getValue(): Short = this.value

    companion object {
        fun ShortEncodedValue.toMutable(): MutableShortEncodedValue = MutableShortEncodedValue(this)
    }
}
