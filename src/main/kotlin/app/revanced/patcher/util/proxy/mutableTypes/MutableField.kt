package app.revanced.patcher.util.proxy.mutableTypes

import app.revanced.patcher.util.proxy.mutableTypes.MutableAnnotation.Companion.toMutable
import app.revanced.patcher.util.proxy.mutableTypes.encodedValue.MutableEncodedValue
import app.revanced.patcher.util.proxy.mutableTypes.encodedValue.MutableEncodedValue.Companion.toMutable
import com.android.tools.smali.dexlib2.HiddenApiRestriction
import com.android.tools.smali.dexlib2.base.reference.BaseFieldReference
import com.android.tools.smali.dexlib2.iface.Field

class MutableField(field: Field) :
    BaseFieldReference(),
    Field {
    private var definingClass = field.definingClass
    private var name = field.name
    private var type = field.type
    private var accessFlags = field.accessFlags

    private var initialValue = field.initialValue?.toMutable()
    private val _annotations by lazy { field.annotations.map { annotation -> annotation.toMutable() }.toMutableSet() }
    private val _hiddenApiRestrictions by lazy { field.hiddenApiRestrictions }

    fun setDefiningClass(definingClass: String) {
        this.definingClass = definingClass
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

    override fun getDefiningClass(): String = this.definingClass

    override fun getName(): String = this.name

    override fun getType(): String = this.type

    override fun getAnnotations(): MutableSet<MutableAnnotation> = this._annotations

    override fun getAccessFlags(): Int = this.accessFlags

    override fun getHiddenApiRestrictions(): MutableSet<HiddenApiRestriction> = this._hiddenApiRestrictions

    override fun getInitialValue(): MutableEncodedValue? = this.initialValue

    companion object {
        fun Field.toMutable(): MutableField = MutableField(this)
    }
}
