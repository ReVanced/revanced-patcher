package app.revanced.com.android.tools.smali.dexlib2.mutable

import app.revanced.com.android.tools.smali.dexlib2.mutable.MutableAnnotationElement
import com.android.tools.smali.dexlib2.base.BaseAnnotation
import com.android.tools.smali.dexlib2.iface.Annotation

class MutableAnnotation(
    annotation: Annotation,
) : BaseAnnotation() {
    private val visibility = annotation.visibility
    private val type = annotation.type
    private val _elements by lazy {
        annotation.elements.map { element -> MutableAnnotationElement(element) }.toMutableSet()
    }

    override fun getType() = type

    override fun getElements() = _elements

    override fun getVisibility() = visibility

    companion object {
        @Deprecated(
            "Use MutableAnnotation constructor instead.",
            ReplaceWith("MutableAnnotation(this)")
        )
        fun Annotation.toMutable() = MutableAnnotation(this)
    }
}
