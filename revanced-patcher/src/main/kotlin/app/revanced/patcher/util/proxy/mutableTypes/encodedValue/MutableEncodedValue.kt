package app.revanced.patcher.util.proxy.mutableTypes.encodedValue

import org.jf.dexlib2.ValueType
import org.jf.dexlib2.iface.value.*

interface MutableEncodedValue : EncodedValue {
    companion object {
        fun EncodedValue.toMutable(): MutableEncodedValue {
            return when (this.valueType) {
                ValueType.TYPE -> MutableTypeEncodedValue(this as TypeEncodedValue)
                ValueType.FIELD -> MutableFieldEncodedValue(this as FieldEncodedValue)
                ValueType.METHOD -> MutableMethodEncodedValue(this as MethodEncodedValue)
                ValueType.ENUM -> MutableEnumEncodedValue(this as EnumEncodedValue)
                ValueType.ARRAY -> MutableArrayEncodedValue(this as ArrayEncodedValue)
                ValueType.ANNOTATION -> MutableAnnotationEncodedValue(this as AnnotationEncodedValue)
                ValueType.BYTE -> MutableByteEncodedValue(this as ByteEncodedValue)
                ValueType.SHORT -> MutableShortEncodedValue(this as ShortEncodedValue)
                ValueType.CHAR -> MutableCharEncodedValue(this as CharEncodedValue)
                ValueType.INT -> MutableIntEncodedValue(this as IntEncodedValue)
                ValueType.LONG -> MutableLongEncodedValue(this as LongEncodedValue)
                ValueType.FLOAT -> MutableFloatEncodedValue(this as FloatEncodedValue)
                ValueType.DOUBLE -> MutableDoubleEncodedValue(this as DoubleEncodedValue)
                ValueType.METHOD_TYPE -> MutableMethodTypeEncodedValue(this as MethodTypeEncodedValue)
                ValueType.METHOD_HANDLE -> MutableMethodHandleEncodedValue(this as MethodHandleEncodedValue)
                ValueType.STRING -> MutableStringEncodedValue(this as StringEncodedValue)
                ValueType.BOOLEAN -> MutableBooleanEncodedValue(this as BooleanEncodedValue)
                ValueType.NULL -> MutableNullEncodedValue()
                else -> this as MutableEncodedValue
            }
        }
    }
}