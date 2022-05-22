package app.revanced.patcher.util.proxy.mutableTypes

import app.revanced.patcher.util.proxy.mutableTypes.MutableAnnotation.Companion.toMutable
import app.revanced.patcher.util.proxy.mutableTypes.MutableField.Companion.toMutable
import app.revanced.patcher.util.proxy.mutableTypes.MutableMethod.Companion.toMutable
import com.google.common.collect.Iterables
import org.jf.dexlib2.base.reference.BaseTypeReference
import org.jf.dexlib2.iface.ClassDef
import org.jf.dexlib2.util.FieldUtil
import org.jf.dexlib2.util.MethodUtil

class MutableClass(classDef: ClassDef) : ClassDef, BaseTypeReference() {
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
    private val _directMethods by lazy { Iterables.filter(_methods, MethodUtil.METHOD_IS_DIRECT).toMutableSet() }
    private val _virtualMethods by lazy { Iterables.filter(_methods, MethodUtil.METHOD_IS_VIRTUAL).toMutableSet() }

    // Fields
    private val _fields by lazy { classDef.fields.map { field -> field.toMutable() }.toMutableSet() }
    private val _staticFields by lazy { Iterables.filter(_fields, FieldUtil.FIELD_IS_STATIC).toMutableSet() }
    private val _instanceFields by lazy { Iterables.filter(_fields, FieldUtil.FIELD_IS_INSTANCE).toMutableSet() }

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

    override fun getType(): String {
        return type
    }

    override fun getAccessFlags(): Int {
        return accessFlags
    }

    override fun getSourceFile(): String? {
        return sourceFile
    }

    override fun getSuperclass(): String? {
        return superclass
    }

    override fun getInterfaces(): MutableList<String> {
        return _interfaces
    }

    override fun getAnnotations(): MutableSet<MutableAnnotation> {
        return _annotations
    }

    override fun getStaticFields(): MutableSet<MutableField> {
        return _staticFields
    }

    override fun getInstanceFields(): MutableSet<MutableField> {
        return _instanceFields
    }

    override fun getFields(): MutableSet<MutableField> {
        return _fields
    }

    override fun getDirectMethods(): MutableSet<MutableMethod> {
        return _directMethods
    }

    override fun getVirtualMethods(): MutableSet<MutableMethod> {
        return _virtualMethods
    }

    override fun getMethods(): MutableSet<MutableMethod> {
        return _methods
    }

    companion object {
        fun ClassDef.toMutable(): MutableClass {
            return MutableClass(this)
        }
    }
}