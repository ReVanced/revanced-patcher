package app.revanced.patcher.cache.proxy.mutableTypes

import app.revanced.patcher.cache.proxy.mutableTypes.MutableAnnotation.Companion.toMutable
import app.revanced.patcher.cache.proxy.mutableTypes.MutableEncodedValue.Companion.toMutable
import org.jf.dexlib2.base.reference.BaseFieldReference
import org.jf.dexlib2.iface.Field

class MutableField(field: Field) : Field, BaseFieldReference() {
    private var definingClass = field.definingClass
    private var name = field.name
    private var type = field.type
    private var accessFlags = field.accessFlags
    private var initialValue = field.initialValue?.toMutable()
    private val annotations = field.annotations.map { annotation -> annotation.toMutable() }.toMutableSet()

    fun setDefiningClass(definingClass: String) {
        this.definingClass
    }

    fun setName(name: String) {
        this.name = name
    }

    fun setType(type: String) {
        this.type = type
    }

    fun setAccessFlags(accessFlags: Int) {
        this.accessFlags = accessFlags
    }

    fun setInitialValue(initialValue: MutableEncodedValue?) {
        this.initialValue = initialValue
    }

    override fun getDefiningClass(): String {
        return this.definingClass
    }

    override fun getName(): String {
        return this.name
    }

    override fun getType(): String {
        return this.type
    }

    override fun getAnnotations(): MutableSet<MutableAnnotation> {
        return this.annotations
    }

    override fun getAccessFlags(): Int {
        return this.accessFlags
    }

    override fun getInitialValue(): MutableEncodedValue? {
        return this.initialValue
    }

    companion object {
        fun Field.toMutable(): MutableField {
            return MutableField(this)
        }
    }
}
