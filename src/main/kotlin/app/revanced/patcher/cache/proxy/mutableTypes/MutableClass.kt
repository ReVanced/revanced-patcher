package app.revanced.patcher.cache.proxy.mutableTypes

import app.revanced.patcher.cache.proxy.mutableTypes.MutableAnnotation.Companion.toMutable
import app.revanced.patcher.cache.proxy.mutableTypes.MutableField.Companion.toMutable
import app.revanced.patcher.cache.proxy.mutableTypes.MutableMethod.Companion.toMutable
import org.jf.dexlib2.base.reference.BaseTypeReference
import org.jf.dexlib2.iface.ClassDef

class MutableClass(classDef: ClassDef) : ClassDef, BaseTypeReference() {
    // Class
    private var type = classDef.type
    private var sourceFile = classDef.sourceFile
    private var accessFlags = classDef.accessFlags
    private var superclass = classDef.superclass

    private val interfaces = classDef.interfaces.toMutableList()
    private val annotations = classDef.annotations.map { annotation -> annotation.toMutable() }.toMutableSet()

    // Methods
    private val methods = classDef.methods.map { method -> method.toMutable() }.toMutableSet()
    private val directMethods = classDef.directMethods.map { directMethod -> directMethod.toMutable() }.toMutableSet()
    private val virtualMethods =
        classDef.virtualMethods.map { virtualMethod -> virtualMethod.toMutable() }.toMutableSet()

    // Fields
    private val fields = classDef.fields.map { field -> field.toMutable() }.toMutableSet()
    private val staticFields = classDef.staticFields.map { staticField -> staticField.toMutable() }.toMutableSet()
    private val instanceFields =
        classDef.instanceFields.map { instanceFields -> instanceFields.toMutable() }.toMutableSet()

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
        return interfaces
    }

    override fun getAnnotations(): MutableSet<MutableAnnotation> {
        return annotations
    }

    override fun getStaticFields(): MutableSet<MutableField> {
        return staticFields
    }

    override fun getInstanceFields(): MutableSet<MutableField> {
        return instanceFields
    }

    override fun getFields(): MutableSet<MutableField> {
        return fields
    }

    override fun getDirectMethods(): MutableSet<MutableMethod> {
        return directMethods
    }

    override fun getVirtualMethods(): MutableSet<MutableMethod> {
        return virtualMethods
    }

    override fun getMethods(): MutableSet<MutableMethod> {
        return methods
    }
}