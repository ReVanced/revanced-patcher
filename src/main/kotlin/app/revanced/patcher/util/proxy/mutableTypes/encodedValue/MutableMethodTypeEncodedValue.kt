package app.revanced.patcher.util.proxy.mutableTypes.encodedValue

import com.android.tools.smali.dexlib2.base.value.BaseMethodTypeEncodedValue
import com.android.tools.smali.dexlib2.iface.reference.MethodProtoReference
import com.android.tools.smali.dexlib2.iface.value.MethodTypeEncodedValue

class MutableMethodTypeEncodedValue(methodTypeEncodedValue: MethodTypeEncodedValue) : BaseMethodTypeEncodedValue(),
    MutableEncodedValue {
    private var value = methodTypeEncodedValue.value

    override fun getValue(): MethodProtoReference {
        return this.value
    }

    fun setValue(value: MethodProtoReference) {
        this.value = value
    }

    companion object {
        fun MethodTypeEncodedValue.toMutable(): MutableMethodTypeEncodedValue {
            return MutableMethodTypeEncodedValue(this)
        }
    }


}