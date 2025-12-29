package com.android.tools.smali.dexlib2.mutable

import com.android.tools.smali.dexlib2.base.BaseAnnotationElement
import com.android.tools.smali.dexlib2.iface.AnnotationElement
import com.android.tools.smali.dexlib2.iface.value.MutableEncodedValue
import com.android.tools.smali.dexlib2.iface.value.MutableEncodedValue.Companion.toMutable

class MutableAnnotationElement(annotationElement: AnnotationElement) : BaseAnnotationElement() {
    private var name = annotationElement.name
    private var value = annotationElement.value.toMutable()

    fun setName(name: String) {
        this.name = name
    }

    fun setValue(value: MutableEncodedValue) {
        this.value = value
    }

    override fun getName() = name

    override fun getValue() = value

    companion object {
        fun AnnotationElement.toMutable() = MutableAnnotationElement(this)
    }
}
