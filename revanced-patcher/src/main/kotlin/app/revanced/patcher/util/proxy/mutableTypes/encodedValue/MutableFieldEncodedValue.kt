package app.revanced.patcher.util.proxy.mutableTypes.encodedValue

import com.android.tools.smali.dexlib2.ValueType
import com.android.tools.smali.dexlib2.base.value.BaseFieldEncodedValue
import com.android.tools.smali.dexlib2.iface.reference.FieldReference
import com.android.tools.smali.dexlib2.iface.value.FieldEncodedValue

class MutableFieldEncodedValue(fieldEncodedValue: FieldEncodedValue) : BaseFieldEncodedValue(), MutableEncodedValue {
    private var value = fieldEncodedValue.value

    override fun getValueType(): Int {
        return ValueType.FIELD
    }

    override fun getValue(): FieldReference {
        return this.value
    }

    fun setValue(value: FieldReference) {
        this.value = value
    }

    companion object {
        fun FieldEncodedValue.toMutable(): MutableFieldEncodedValue {
            return MutableFieldEncodedValue(this)
        }
    }
}
