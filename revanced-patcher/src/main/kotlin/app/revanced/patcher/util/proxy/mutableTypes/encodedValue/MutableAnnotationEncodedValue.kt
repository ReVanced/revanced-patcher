package app.revanced.patcher.util.proxy.mutableTypes.encodedValue

import app.revanced.patcher.util.proxy.mutableTypes.MutableAnnotationElement.Companion.toMutable
import com.android.tools.smali.dexlib2.base.value.BaseAnnotationEncodedValue
import com.android.tools.smali.dexlib2.iface.AnnotationElement
import com.android.tools.smali.dexlib2.iface.value.AnnotationEncodedValue

class MutableAnnotationEncodedValue(annotationEncodedValue: AnnotationEncodedValue) : BaseAnnotationEncodedValue(),
    MutableEncodedValue {
    private var type = annotationEncodedValue.type

    private val _elements by lazy {
        annotationEncodedValue.elements.map { annotationElement -> annotationElement.toMutable() }.toMutableSet()
    }

    override fun getType(): String {
        return this.type
    }

    fun setType(type: String) {
        this.type = type
    }

    override fun getElements(): MutableSet<out AnnotationElement> {
        return _elements
    }

    companion object {
        fun AnnotationEncodedValue.toMutable(): MutableAnnotationEncodedValue {
            return MutableAnnotationEncodedValue(this)
        }
    }
}
