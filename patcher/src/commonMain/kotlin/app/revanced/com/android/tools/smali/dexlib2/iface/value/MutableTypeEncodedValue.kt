package app.revanced.com.android.tools.smali.dexlib2.iface.value

import com.android.tools.smali.dexlib2.base.value.BaseTypeEncodedValue
import com.android.tools.smali.dexlib2.iface.value.TypeEncodedValue

class MutableTypeEncodedValue(
    typeEncodedValue: TypeEncodedValue,
) : BaseTypeEncodedValue(),
    MutableEncodedValue {
    private var value = typeEncodedValue.value

    fun setValue(value: String) {
        this.value = value
    }

    override fun getValue() = this.value

    companion object {
        fun TypeEncodedValue.toMutable(): MutableTypeEncodedValue = MutableTypeEncodedValue(this)
    }
}
