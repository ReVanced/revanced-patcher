package app.revanced.com.android.tools.smali.dexlib2.iface.value

import com.android.tools.smali.dexlib2.base.value.BaseLongEncodedValue
import com.android.tools.smali.dexlib2.iface.value.LongEncodedValue

class MutableLongEncodedValue(
    longEncodedValue: LongEncodedValue,
) : BaseLongEncodedValue(),
    MutableEncodedValue {
    private var value = longEncodedValue.value

    fun setValue(value: Long) {
        this.value = value
    }

    override fun getValue(): Long = this.value

    companion object {
        fun LongEncodedValue.toMutable(): MutableLongEncodedValue = MutableLongEncodedValue(this)
    }
}
