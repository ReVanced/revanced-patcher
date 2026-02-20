package app.revanced.com.android.tools.smali.dexlib2.iface.value

import app.revanced.com.android.tools.smali.dexlib2.iface.value.MutableEncodedValue.Companion.toMutable
import com.android.tools.smali.dexlib2.base.value.BaseArrayEncodedValue
import com.android.tools.smali.dexlib2.iface.value.ArrayEncodedValue

class MutableArrayEncodedValue(
    arrayEncodedValue: ArrayEncodedValue,
) : BaseArrayEncodedValue(),
    MutableEncodedValue {
    private val _value by lazy {
        arrayEncodedValue.value.map { encodedValue -> encodedValue.toMutable() }.toMutableList()
    }

    override fun getValue() = _value

    companion object {
        fun ArrayEncodedValue.toMutable(): MutableArrayEncodedValue = MutableArrayEncodedValue(this)
    }
}
