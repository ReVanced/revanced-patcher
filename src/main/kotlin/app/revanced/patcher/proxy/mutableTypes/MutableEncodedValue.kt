package app.revanced.patcher.proxy.mutableTypes

import org.jf.dexlib2.iface.value.EncodedValue

// TODO: We need to create implementations for the interfaces
//  TypeEncodedValue, FieldEncodedValue, MethodEncodedValue,
//  EnumEncodedValue, ArrayEncodedValue and AnnotationEncodedValue or the cast back to the immutable type will fail
class MutableEncodedValue(encodedValue: EncodedValue) : EncodedValue {
    private var valueType = encodedValue.valueType

    fun setValueType(valueType: Int) {
        this.valueType = valueType
    }

    override fun compareTo(other: EncodedValue): Int {
        return valueType - other.valueType
    }

    override fun getValueType(): Int {
        return valueType
    }

    companion object {
        fun EncodedValue.toMutable(): MutableEncodedValue {
            return MutableEncodedValue(this)
        }
    }
}