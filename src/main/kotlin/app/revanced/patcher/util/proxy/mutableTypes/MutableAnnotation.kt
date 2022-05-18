package app.revanced.patcher.util.proxy.mutableTypes

import app.revanced.patcher.util.proxy.mutableTypes.MutableAnnotationElement.Companion.toMutable
import org.jf.dexlib2.base.BaseAnnotation
import org.jf.dexlib2.iface.Annotation

class MutableAnnotation(annotation: Annotation) : BaseAnnotation() {
    private val visibility = annotation.visibility
    private val type = annotation.type
    private val _elements by lazy { annotation.elements.map { element -> element.toMutable() }.toMutableSet() }

    override fun getType(): String {
        return type
    }

    override fun getElements(): MutableSet<MutableAnnotationElement> {
        return _elements
    }

    override fun getVisibility(): Int {
        return visibility
    }

    companion object {
        fun Annotation.toMutable(): MutableAnnotation {
            return MutableAnnotation(this)
        }
    }
}
