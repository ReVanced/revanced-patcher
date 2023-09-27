package app.revanced.patcher.util.proxy.mutableTypes.encodedValue

import com.android.tools.smali.dexlib2.base.value.BaseMethodEncodedValue
import com.android.tools.smali.dexlib2.iface.reference.MethodReference
import com.android.tools.smali.dexlib2.iface.value.MethodEncodedValue

class MutableMethodEncodedValue(methodEncodedValue: MethodEncodedValue) : BaseMethodEncodedValue(),
    MutableEncodedValue {
    private var value = methodEncodedValue.value

    override fun getValue(): MethodReference {
        return this.value
    }

    fun setValue(value: MethodReference) {
        this.value = value
    }

    companion object {
        fun MethodEncodedValue.toMutable(): MutableMethodEncodedValue {
            return MutableMethodEncodedValue(this)
        }
    }
}
