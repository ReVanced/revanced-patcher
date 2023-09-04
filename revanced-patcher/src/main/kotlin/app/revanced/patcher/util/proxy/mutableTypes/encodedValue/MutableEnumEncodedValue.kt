package app.revanced.patcher.util.proxy.mutableTypes.encodedValue

import com.android.tools.smali.dexlib2.base.value.BaseEnumEncodedValue
import com.android.tools.smali.dexlib2.iface.reference.FieldReference
import com.android.tools.smali.dexlib2.iface.value.EnumEncodedValue

class MutableEnumEncodedValue(enumEncodedValue: EnumEncodedValue) : BaseEnumEncodedValue(), MutableEncodedValue {
    private var value = enumEncodedValue.value

    override fun getValue(): FieldReference {
        return this.value
    }

    fun setValue(value: FieldReference) {
        this.value = value
    }

    companion object {
        fun EnumEncodedValue.toMutable(): MutableEnumEncodedValue {
            return MutableEnumEncodedValue(this)
        }
    }
}
