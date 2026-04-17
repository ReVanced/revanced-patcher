package app.revanced.com.android.tools.smali.dexlib2.iface.value

import app.revanced.com.android.tools.smali.dexlib2.iface.value.MutableEncodedValue.Companion.toMutableEncodedValue
import com.android.tools.smali.dexlib2.base.value.BaseArrayEncodedValue
import com.android.tools.smali.dexlib2.iface.value.ArrayEncodedValue

class MutableArrayEncodedValue(
    arrayEncodedValue: ArrayEncodedValue,
) : BaseArrayEncodedValue(),
    MutableEncodedValue {
    private val _value by lazy {
        arrayEncodedValue.value.map { encodedValue -> encodedValue.toMutableEncodedValue() }.toMutableList()
    }

    override fun getValue() = _value

    companion object {
        @Deprecated(
            "Use MutableArrayEncodedValue constructor instead.",
            ReplaceWith("MutableArrayEncodedValue(this)")
        )
        fun ArrayEncodedValue.toMutable(): MutableArrayEncodedValue = MutableArrayEncodedValue(this)
    }
}
