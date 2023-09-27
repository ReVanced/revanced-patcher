package app.revanced.patcher.util.proxy.mutableTypes.encodedValue

import com.android.tools.smali.dexlib2.base.value.BaseMethodHandleEncodedValue
import com.android.tools.smali.dexlib2.iface.reference.MethodHandleReference
import com.android.tools.smali.dexlib2.iface.value.MethodHandleEncodedValue

class MutableMethodHandleEncodedValue(methodHandleEncodedValue: MethodHandleEncodedValue) :
    BaseMethodHandleEncodedValue(),
    MutableEncodedValue {
    private var value = methodHandleEncodedValue.value

    override fun getValue(): MethodHandleReference {
        return this.value
    }

    fun setValue(value: MethodHandleReference) {
        this.value = value
    }

    companion object {
        fun MethodHandleEncodedValue.toMutable(): MutableMethodHandleEncodedValue {
            return MutableMethodHandleEncodedValue(this)
        }
    }


}