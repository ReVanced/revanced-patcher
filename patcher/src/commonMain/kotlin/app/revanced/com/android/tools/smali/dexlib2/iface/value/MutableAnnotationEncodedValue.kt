package app.revanced.com.android.tools.smali.dexlib2.iface.value

import app.revanced.com.android.tools.smali.dexlib2.mutable.MutableAnnotationElement.Companion.toMutable
import com.android.tools.smali.dexlib2.base.value.BaseAnnotationEncodedValue
import com.android.tools.smali.dexlib2.iface.value.AnnotationEncodedValue

class MutableAnnotationEncodedValue(
    annotationEncodedValue: AnnotationEncodedValue,
) : BaseAnnotationEncodedValue(),
    MutableEncodedValue {
    private var type = annotationEncodedValue.type

    private val _elements by lazy {
        annotationEncodedValue.elements.map { annotationElement -> annotationElement.toMutable() }.toMutableSet()
    }

    fun setType(type: String) {
        this.type = type
    }

    override fun getType() = this.type

    override fun getElements() = _elements

    companion object {
        fun AnnotationEncodedValue.toMutable() = MutableAnnotationEncodedValue(this)
    }
}
