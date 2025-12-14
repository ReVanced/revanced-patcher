package com.android.tools.smali.dexlib2.mutable

import com.android.tools.smali.dexlib2.base.reference.BaseTypeReference
import com.android.tools.smali.dexlib2.iface.ClassDef
import com.android.tools.smali.dexlib2.mutable.MutableAnnotation.Companion.toMutable
import com.android.tools.smali.dexlib2.mutable.MutableField.Companion.toMutable
import com.android.tools.smali.dexlib2.mutable.MutableMethod.Companion.toMutable
import com.android.tools.smali.dexlib2.util.FieldUtil
import com.android.tools.smali.dexlib2.util.MethodUtil

class MutableClassDef(classDef: ClassDef) : BaseTypeReference(), ClassDef {
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
    private val _directMethods by lazy { methods.filter { method -> MethodUtil.isDirect(method) }.toMutableSet() }
    private val _virtualMethods by lazy { methods.filter { method -> !MethodUtil.isDirect(method) }.toMutableSet() }

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

    override fun getType() = type

    override fun getAccessFlags() = accessFlags

    override fun getSourceFile() = sourceFile

    override fun getSuperclass() = superclass

    override fun getInterfaces() = _interfaces

    override fun getAnnotations() = _annotations

    override fun getStaticFields() = _staticFields

    override fun getInstanceFields() = _instanceFields

    override fun getFields() = _fields

    override fun getDirectMethods() = _directMethods

    override fun getVirtualMethods() = _virtualMethods

    override fun getMethods() = _methods

    companion object {
        fun ClassDef.toMutable(): MutableClassDef = MutableClassDef(this)
    }
}
