package app.revanced.patcher.util.proxy.mutableTypes

import app.revanced.patcher.util.proxy.mutableTypes.MutableAnnotationElement.Companion.toMutable
import com.android.tools.smali.dexlib2.base.BaseAnnotation
import com.android.tools.smali.dexlib2.iface.Annotation

class MutableAnnotation(annotation: Annotation) : BaseAnnotation() {
    private val visibility = annotation.visibility
    private val type = annotation.type
    private val _elements by lazy { annotation.elements.map { element -> element.toMutable() }.toMutableSet() }

    override fun getType(): String = type

    override fun getElements(): MutableSet<MutableAnnotationElement> = _elements

    override fun getVisibility(): Int = visibility

    companion object {
        fun Annotation.toMutable(): MutableAnnotation = MutableAnnotation(this)
    }
}
