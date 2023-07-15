package app.revanced.patcher.util.proxy.mutableTypes.encodedValue

import org.jf.dexlib2.base.value.BaseTypeEncodedValue
import org.jf.dexlib2.iface.value.TypeEncodedValue

class MutableTypeEncodedValue(typeEncodedValue: TypeEncodedValue) : BaseTypeEncodedValue(), MutableEncodedValue {
    private var value = typeEncodedValue.value

    override fun getValue(): String {
        return this.value
    }

    fun setValue(value: String) {
        this.value = value
    }

    companion object {
        fun TypeEncodedValue.toMutable(): MutableTypeEncodedValue {
            return MutableTypeEncodedValue(this)
        }
    }
}
