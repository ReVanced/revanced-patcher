package app.revanced.patcher.util.proxy.mutableTypes

import app.revanced.patcher.util.proxy.mutableTypes.MutableAnnotation.Companion.toMutable
import app.revanced.patcher.util.proxy.mutableTypes.MutableMethodParameter.Companion.toMutable
import org.jf.dexlib2.HiddenApiRestriction
import org.jf.dexlib2.base.reference.BaseMethodReference
import org.jf.dexlib2.builder.MutableMethodImplementation
import org.jf.dexlib2.iface.Method

class MutableMethod(method: Method) : Method, BaseMethodReference() {
    private var definingClass = method.definingClass
    private var name = method.name
    private var accessFlags = method.accessFlags
    private var returnType = method.returnType

    // Create own mutable MethodImplementation (due to not being able to change members like register count).
    private val _implementation by lazy { method.implementation?.let { MutableMethodImplementation(it) } }
    private val _annotations by lazy { method.annotations.map { annotation -> annotation.toMutable() }.toMutableSet() }
    private val _parameters by lazy { method.parameters.map { parameter -> parameter.toMutable() }.toMutableList() }
    private val _parameterTypes by lazy { method.parameterTypes.toMutableList() }
    private val _hiddenApiRestrictions by lazy { method.hiddenApiRestrictions }

    fun setDefiningClass(definingClass: String) {
        this.definingClass = definingClass
    }

    fun setName(name: String) {
        this.name = name
    }

    fun setAccessFlags(accessFlags: Int) {
        this.accessFlags = accessFlags
    }

    fun setReturnType(returnType: String) {
        this.returnType = returnType
    }

    override fun getDefiningClass(): String {
        return definingClass
    }

    override fun getName(): String {
        return name
    }

    override fun getParameterTypes(): MutableList<CharSequence> {
        return _parameterTypes
    }

    override fun getReturnType(): String {
        return returnType
    }

    override fun getAnnotations(): MutableSet<MutableAnnotation> {
        return _annotations
    }

    override fun getAccessFlags(): Int {
        return accessFlags
    }

    override fun getHiddenApiRestrictions(): MutableSet<HiddenApiRestriction> {
        return _hiddenApiRestrictions
    }

    override fun getParameters(): MutableList<MutableMethodParameter> {
        return _parameters
    }

    override fun getImplementation(): MutableMethodImplementation? {
        return _implementation
    }

    companion object {
        fun Method.toMutable(): MutableMethod {
            return MutableMethod(this)
        }
    }
}