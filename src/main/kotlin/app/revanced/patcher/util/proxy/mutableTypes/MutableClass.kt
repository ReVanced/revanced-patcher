package app.revanced.patcher.util.proxy.mutableTypes

import app.revanced.patcher.util.proxy.mutableTypes.MutableAnnotation.Companion.toMutable
import app.revanced.patcher.util.proxy.mutableTypes.MutableField.Companion.toMutable
import app.revanced.patcher.util.proxy.mutableTypes.MutableMethod.Companion.toMutable
import com.android.tools.smali.dexlib2.base.reference.BaseTypeReference
import com.android.tools.smali.dexlib2.iface.ClassDef
import com.android.tools.smali.dexlib2.util.FieldUtil
import com.android.tools.smali.dexlib2.util.MethodUtil

class MutableClass(classDef: ClassDef) :
    BaseTypeReference(),
    ClassDef {
    // Class
    private var type = classDef.type
    private var sourceFile = classDef.sourceFile
    private var accessFlags = classDef.accessFlags
    private var superclass = classDef.superclass

    private val _interfaces by lazy { classDef.interfaces.toMutableList() }
    private val _annotations by lazy {
        classDef.annotations.map { annotation -> annotation.toMutable() }.toMutableSet()
    }

    // Methods
    private val _methods by lazy { classDef.methods.map { method -> method.toMutable() }.toMutableSet() }
    private val _directMethods by lazy { _methods.filter { method -> MethodUtil.isDirect(method) }.toMutableSet() }
    private val _virtualMethods by lazy { _methods.filter { method -> !MethodUtil.isDirect(method) }.toMutableSet() }

    // Fields
    private val _fields by lazy { classDef.fields.map { field -> field.toMutable() }.toMutableSet() }
    private val _staticFields by lazy { _fields.filter { field -> FieldUtil.isStatic(field) }.toMutableSet() }
    private val _instanceFields by lazy { _fields.filter { field -> !FieldUtil.isStatic(field) }.toMutableSet() }

    fun setType(type: String) {
        this.type = type
    }

    fun setSourceFile(sourceFile: String?) {
        this.sourceFile = sourceFile
    }

    fun setAccessFlags(accessFlags: Int) {
        this.accessFlags = accessFlags
    }

    fun setSuperClass(superclass: String?) {
        this.superclass = superclass
    }

    override fun getType(): String = type

    override fun getAccessFlags(): Int = accessFlags

    override fun getSourceFile(): String? = sourceFile

    override fun getSuperclass(): String? = superclass

    override fun getInterfaces(): MutableList<String> = _interfaces

    override fun getAnnotations(): MutableSet<MutableAnnotation> = _annotations

    override fun getStaticFields(): MutableSet<MutableField> = _staticFields

    override fun getInstanceFields(): MutableSet<MutableField> = _instanceFields

    override fun getFields(): MutableSet<MutableField> = _fields

    override fun getDirectMethods(): MutableSet<MutableMethod> = _directMethods

    override fun getVirtualMethods(): MutableSet<MutableMethod> = _virtualMethods

    override fun getMethods(): MutableSet<MutableMethod> = _methods

    companion object {
        fun ClassDef.toMutable(): MutableClass = MutableClass(this)
    }
}
