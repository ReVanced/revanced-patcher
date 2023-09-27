package app.revanced.patcher.util.proxy.mutableTypes.encodedValue

import com.android.tools.smali.dexlib2.base.value.BaseLongEncodedValue
import com.android.tools.smali.dexlib2.iface.value.LongEncodedValue

class MutableLongEncodedValue(longEncodedValue: LongEncodedValue) : BaseLongEncodedValue(), MutableEncodedValue {
    private var value = longEncodedValue.value

    override fun getValue(): Long {
        return this.value
    }

    fun setValue(value: Long) {
        this.value = value
    }

    companion object {
        fun LongEncodedValue.toMutable(): MutableLongEncodedValue {
            return MutableLongEncodedValue(this)
        }
    }
}
