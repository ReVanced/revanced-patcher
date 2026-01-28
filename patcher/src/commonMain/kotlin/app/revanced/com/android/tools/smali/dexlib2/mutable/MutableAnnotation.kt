package app.revanced.com.android.tools.smali.dexlib2.mutable

import app.revanced.com.android.tools.smali.dexlib2.mutable.MutableAnnotationElement.Companion.toMutable
import com.android.tools.smali.dexlib2.base.BaseAnnotation
import com.android.tools.smali.dexlib2.iface.Annotation

class MutableAnnotation(
    annotation: Annotation,
) : BaseAnnotation() {
    private val visibility = annotation.visibility
    private val type = annotation.type
    private val _elements by lazy { annotation.elements.map { element -> element.toMutable() }.toMutableSet() }

    override fun getType() = type

    override fun getElements() = _elements

    override fun getVisibility() = visibility

    companion object {
        fun Annotation.toMutable() = MutableAnnotation(this)
    }
}
