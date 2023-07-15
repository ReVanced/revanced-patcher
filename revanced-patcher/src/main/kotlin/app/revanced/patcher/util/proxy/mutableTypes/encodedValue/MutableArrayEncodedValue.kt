package app.revanced.patcher.util.proxy.mutableTypes.encodedValue

import app.revanced.patcher.util.proxy.mutableTypes.encodedValue.MutableEncodedValue.Companion.toMutable
import org.jf.dexlib2.base.value.BaseArrayEncodedValue
import org.jf.dexlib2.iface.value.ArrayEncodedValue
import org.jf.dexlib2.iface.value.EncodedValue

class MutableArrayEncodedValue(arrayEncodedValue: ArrayEncodedValue) : BaseArrayEncodedValue(), MutableEncodedValue {
    private val _value by lazy {
        arrayEncodedValue.value.map { encodedValue -> encodedValue.toMutable() }.toMutableList()
    }

    override fun getValue(): MutableList<out EncodedValue> {
        return _value
    }

    companion object {
        fun ArrayEncodedValue.toMutable(): MutableArrayEncodedValue {
            return MutableArrayEncodedValue(this)
        }
    }
}
